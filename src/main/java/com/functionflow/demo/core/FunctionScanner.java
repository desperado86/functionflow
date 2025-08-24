package com.functionflow.demo.core;

import com.functionflow.demo.annotation.Input;
import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Output;
import com.functionflow.demo.annotation.Functions;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.functionflow.demo.model.FunctionSchema;
import com.functionflow.demo.model.ParameterSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * 函数扫描服务
 */
@Slf4j
@Service
public class FunctionScanner {

    private final Map<String, FunctionSchema> functionRegistry = new ConcurrentHashMap<>();
    private final Map<String, Method> methodRegistry = new ConcurrentHashMap<>();
    private final JsonSchemaGenerator jsonSchemaGenerator;

    @Autowired
    public FunctionScanner(JsonSchemaGenerator jsonSchemaGenerator) {
        this.jsonSchemaGenerator = jsonSchemaGenerator;
    }

    /**
     * 发现并注册类中的所有函数
     */
    public void discoverFunctions(Class<?> clazz) {
        // 检查类是否有Functions注解
        Functions functions = clazz.getAnnotation(Functions.class);
        if (functions != null && !functions.enabled()) {
            log.info("跳过禁用的函数集合: {}", clazz.getSimpleName());
            return;
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Function functionNode = method.getAnnotation(Function.class);
            if (functionNode != null) {
                registerFunction(clazz, method, functionNode);
            }
        }
    }

    /**
     * 发现并注册类中的所有函数（包括无注解的函数）
     */
    public void discoverAllFunctions(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Function functionNode = method.getAnnotation(Function.class);
            if (functionNode != null) {
                // 有注解的函数
                registerFunction(clazz, method, functionNode);
            } else {
                // 无注解的函数，创建默认的Function注解
                registerFunctionWithoutAnnotation(clazz, method);
            }
        }
    }

    /**
     * 注册无注解的函数
     */
    public void registerFunctionWithoutAnnotation(Class<?> clazz, Method method) {
        try {
            // 检查方法是否适合作为函数
            if (isSuitableForFunction(method)) {
                FunctionSchema schema = buildFunctionSchemaWithoutAnnotation(clazz, method);
                String functionId = generateFunctionId(clazz, method);

                functionRegistry.put(functionId, schema);
                methodRegistry.put(functionId, method);

                log.info("注册无注解函数: {} -> {}", functionId, schema.getName());
            }
        } catch (Exception e) {
            log.error("注册无注解函数失败: {}.{}", clazz.getSimpleName(), method.getName(), e);
        }
    }

    /**
     * 检查方法是否适合作为函数
     */
    private boolean isSuitableForFunction(Method method) {
        // 排除一些不适合的方法
        String methodName = method.getName();

        // 排除常见的非函数方法
        if (methodName.equals("toString") ||
                methodName.equals("equals") ||
                methodName.equals("hashCode") ||
                methodName.equals("getClass") ||
                methodName.equals("wait") ||
                methodName.equals("notify") ||
                methodName.equals("notifyAll") ||
                methodName.equals("finalize")) {
            return false;
        }

        // 排除私有方法
        if (Modifier.isPrivate(method.getModifiers())) {
            return false;
        }

        // 排除参数过多的方法（通常不是简单的函数）
        if (method.getParameterCount() > 10) {
            return false;
        }

        return true;
    }

    /**
     * 扫描指定包下的所有类
     */
    public void scanPackage(String packageName) {
        try {
            log.info("开始扫描包: {}", packageName);
            Set<Class<?>> classes = findClassesInPackage(packageName);

            for (Class<?> clazz : classes) {
                Functions functions = clazz.getAnnotation(Functions.class);
                if (functions != null) {
                    log.info("发现函数集合: {} -> {}", clazz.getSimpleName(),
                            functions.name().isEmpty() ? clazz.getSimpleName() : functions.name());
                    discoverFunctions(clazz);
                }
            }

            log.info("包扫描完成: {}，发现 {} 个函数集合", packageName, classes.size());
        } catch (Exception e) {
            log.error("扫描包失败: {}", packageName, e);
        }
    }

    /**
     * 扫描多个包
     */
    public void scanPackages(String... packageNames) {
        for (String packageName : packageNames) {
            scanPackage(packageName);
        }
    }

    /**
     * 查找包中的所有类
     */
    private Set<Class<?>> findClassesInPackage(String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                log.warn("包路径不存在: {}", packageName);
                return classes;
            }

            if (resource.getProtocol().equals("file")) {
                // 从文件系统加载
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    findClassesInDirectory(directory, packageName, classes);
                }
            } else if (resource.getProtocol().equals("jar")) {
                // 从JAR文件加载
                findClassesInJar(resource, packageName, classes);
            }
        } catch (Exception e) {
            log.error("查找包中的类失败: {}", packageName, e);
        }
        return classes;
    }

    /**
     * 从目录中查找类
     */
    private void findClassesInDirectory(File directory, String packageName, Set<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + fileName, classes);
            } else if (fileName.endsWith(".class")) {
                String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    log.warn("无法加载类: {}", className);
                }
            }
        }
    }

    /**
     * 从JAR文件中查找类
     */
    private void findClassesInJar(URL resource, String packageName, Set<Class<?>> classes) {
        try {
            String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
            String packagePath = packageName.replace('.', '/');

            try (JarInputStream jarStream = new JarInputStream(new File(jarPath).toURI().toURL().openStream())) {
                JarEntry entry;
                while ((entry = jarStream.getNextJarEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith(packagePath) && name.endsWith(".class")) {
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            log.warn("无法加载类: {}", className);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("从JAR文件查找类失败: {}", packageName, e);
        }
    }

    /**
     * 注册单个函数
     */
    public void registerFunction(Class<?> clazz, Method method, Function function) {
        try {
            FunctionSchema schema = buildFunctionSchema(clazz, method, function);
            String functionId = generateFunctionId(clazz, method);

            functionRegistry.put(functionId, schema);
            methodRegistry.put(functionId, method);

            log.info("注册函数: {} -> {}", functionId, schema.getName());
        } catch (Exception e) {
            log.error("注册函数失败: {}.{}", clazz.getSimpleName(), method.getName(), e);
        }
    }

    /**
     * 构建函数模式
     */
    private FunctionSchema buildFunctionSchema(Class<?> clazz, Method method, Function function) {
        List<ParameterSchema> inputs = buildInputSchemas(method);
        List<ParameterSchema> outputs = buildOutputSchemas(method);
        Map<String, Object> validation = buildValidationSchema(method);

        // 获取类上的Functions注解
        Functions functions = clazz.getAnnotation(Functions.class);

        String functionName = function.name().isEmpty() ? method.getName() : function.name();

        // 继承category：如果Function中没有设置，则从Functions中获取
        String category = function.category().isEmpty() && functions != null ? functions.category()
                : function.category();

        // 继承version：如果Function中没有设置，则从Functions中获取
        String version = function.version().isEmpty() && functions != null ? functions.version() : function.version();

        // 处理命名空间：如果Function中没有设置，则从Functions中获取，如果都是空则使用类全限定名
        String namespace = function.namespace().isEmpty()
                ? (functions != null && !functions.namespace().isEmpty() ? functions.namespace() : clazz.getName())
                : function.namespace();

        return FunctionSchema.builder()
                .id(generateFunctionId(clazz, method))
                .name(functionName)
                .namespace(namespace)
                .description(function.description())
                .category(category)
                .version(version)
                .async(function.async())
                .timeout(function.timeout())
                .cacheable(function.cacheable())
                .cacheTime(function.cacheTime())
                .inputs(inputs)
                .outputs(outputs)
                .validation(validation)
                .build();
    }

    /**
     * 为无注解函数构建函数模式
     */
    private FunctionSchema buildFunctionSchemaWithoutAnnotation(Class<?> clazz, Method method) {
        List<ParameterSchema> inputs = buildInputSchemasWithoutAnnotation(method);
        List<ParameterSchema> outputs = buildOutputSchemasWithoutAnnotation(method);
        Map<String, Object> validation = buildValidationSchema(method);

        // 获取类上的Functions注解
        Functions functions = clazz.getAnnotation(Functions.class);

        String functionName = method.getName();
        String description = generateDefaultDescription(method);

        // 继承category：如果Functions中有设置，则使用
        String category = functions != null ? functions.category() : "通用函数";

        // 继承version：如果Functions中有设置，则使用
        String version = functions != null ? functions.version() : "1.0.0";

        // 处理命名空间：如果Functions中有设置，则使用，否则使用类全限定名
        String namespace = functions != null && !functions.namespace().isEmpty() ? functions.namespace()
                : clazz.getName();

        return FunctionSchema.builder()
                .id(generateFunctionId(clazz, method))
                .name(functionName)
                .namespace(namespace)
                .description(description)
                .category(category)
                .version(version)
                .async(false)
                .timeout(30000)
                .cacheable(false)
                .cacheTime(300)
                .inputs(inputs)
                .outputs(outputs)
                .validation(validation)
                .build();
    }

    /**
     * 构建输入参数模式
     */
    private List<ParameterSchema> buildInputSchemas(Method method) {
        List<ParameterSchema> schemas = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Input input = parameter.getAnnotation(Input.class);

            String paramName = input != null ? input.name() : parameter.getName();
            String paramType = input != null && input.type() != Object.class
                    ? input.type().getName()
                    : parameter.getType().getName();

            // 生成JSON Schema
            ObjectNode jsonSchema = null;
            try {
                // 为所有类型生成JSON Schema，包括基本类型
                jsonSchema = jsonSchemaGenerator.generateSchema(parameter.getType(), true);
            } catch (Exception e) {
                log.warn("生成JSON Schema失败: {}", parameter.getType().getName(), e);
            }

            ParameterSchema schema = ParameterSchema.builder()
                    .name(paramName)
                    .description(input != null ? input.description() : "")
                    .type(paramType)
                    .required(input == null || input.required())
                    .defaultValue(input != null ? input.defaultValue() : null)
                    .order(i)
                    .validation(buildParameterValidation(parameter))
                    .jsonSchema(jsonSchema)
                    .build();

            schemas.add(schema);
        }

        // 按order排序
        schemas.sort(Comparator.comparing(ParameterSchema::getOrder));
        return schemas;
    }

    /**
     * 构建输出参数模式
     */
    private List<ParameterSchema> buildOutputSchemas(Method method) {
        List<ParameterSchema> schemas = new ArrayList<>();

        // 检查方法上的@Output注解
        Output methodOutput = method.getAnnotation(Output.class);

        // 检查返回值上的@Output注解（通过反射获取）
        Output returnOutput = null;
        try {
            // 获取方法的返回类型注解
            java.lang.reflect.AnnotatedType returnType = method.getAnnotatedReturnType();
            if (returnType != null) {
                returnOutput = returnType.getAnnotation(Output.class);
            }
        } catch (Exception e) {
            log.debug("无法获取返回值注解: {}", e.getMessage());
        }

        // 优先使用返回值上的注解，如果没有则使用方法上的注解
        Output output = returnOutput != null ? returnOutput : methodOutput;

        if (output != null) {
            String outputName = output.name().isEmpty() ? "result" : output.name();
            String outputType = output.type() != Object.class ? output.type().getName()
                    : method.getReturnType().getName();

            // 生成JSON Schema
            ObjectNode jsonSchema = null;
            try {
                // 为所有类型生成JSON Schema，包括基本类型
                jsonSchema = jsonSchemaGenerator.generateSchema(method.getReturnType(), true);
            } catch (Exception e) {
                log.warn("生成JSON Schema失败: {}", method.getReturnType().getName(), e);
            }

            ParameterSchema schema = ParameterSchema.builder()
                    .name(outputName)
                    .description(output.description())
                    .type(outputType)
                    .required(output.required())
                    .order(0)
                    .jsonSchema(jsonSchema)
                    .build();

            schemas.add(schema);
        } else {
            // 默认输出
            // 生成JSON Schema
            ObjectNode jsonSchema = null;
            try {
                // 为所有类型生成JSON Schema，包括基本类型
                jsonSchema = jsonSchemaGenerator.generateSchema(method.getReturnType(), true);
            } catch (Exception e) {
                log.warn("生成JSON Schema失败: {}", method.getReturnType().getName(), e);
            }

            ParameterSchema schema = ParameterSchema.builder()
                    .name("result")
                    .description("函数执行结果")
                    .type(method.getReturnType().getName())
                    .required(true)
                    .order(0)
                    .jsonSchema(jsonSchema)
                    .build();

            schemas.add(schema);
        }

        return schemas;
    }

    /**
     * 构建参数验证模式
     */
    private Map<String, Object> buildParameterValidation(Parameter parameter) {
        Map<String, Object> validation = new HashMap<>();

        // 检查验证注解
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof NotNull) {
                validation.put("required", true);
            } else if (annotation instanceof Min) {
                validation.put("min", ((Min) annotation).value());
            } else if (annotation instanceof Max) {
                validation.put("max", ((Max) annotation).value());
            } else if (annotation instanceof Size) {
                Size size = (Size) annotation;
                validation.put("minSize", size.min());
                validation.put("maxSize", size.max());
            } else if (annotation instanceof Pattern) {
                validation.put("pattern", ((Pattern) annotation).regexp());
            } else if (annotation instanceof Email) {
                validation.put("email", true);
            } else if (annotation instanceof DecimalMin) {
                validation.put("decimalMin", ((DecimalMin) annotation).value());
            } else if (annotation instanceof DecimalMax) {
                validation.put("decimalMax", ((DecimalMax) annotation).value());
            } else if (annotation instanceof Digits) {
                Digits digits = (Digits) annotation;
                validation.put("integer", digits.integer());
                validation.put("fraction", digits.fraction());
            } else if (annotation instanceof AssertTrue) {
                validation.put("assertTrue", true);
            } else if (annotation instanceof AssertFalse) {
                validation.put("assertFalse", true);
            } else if (annotation instanceof Future) {
                validation.put("future", true);
            } else if (annotation instanceof Past) {
                validation.put("past", true);
            } else if (annotation instanceof FutureOrPresent) {
                validation.put("futureOrPresent", true);
            } else if (annotation instanceof PastOrPresent) {
                validation.put("pastOrPresent", true);
            }
        }

        return validation;
    }

    /**
     * 构建函数验证模式
     */
    private Map<String, Object> buildValidationSchema(Method method) {
        Map<String, Object> validation = new HashMap<>();

        // 这里可以添加函数级别的验证规则
        // 例如：参数组合验证、业务规则验证等

        return validation;
    }

    /**
     * 生成函数ID（完全限定名）
     */
    private String generateFunctionId(Class<?> clazz, Method method) {
        return clazz.getName() + "." + method.getName();
    }

    /**
     * 获取所有函数
     */
    public List<FunctionSchema> getAllFunctions() {
        return new ArrayList<>(functionRegistry.values());
    }

    /**
     * 根据ID获取函数
     */
    public FunctionSchema getFunction(String functionId) {
        return functionRegistry.get(functionId);
    }

    /**
     * 获取函数方法
     */
    public Method getFunctionMethod(String functionId) {
        return methodRegistry.get(functionId);
    }

    /**
     * 获取函数数量
     */
    public int getFunctionCount() {
        return functionRegistry.size();
    }

    /**
     * 为无注解函数构建输入参数模式
     */
    private List<ParameterSchema> buildInputSchemasWithoutAnnotation(Method method) {
        List<ParameterSchema> schemas = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            String paramName = parameter.getName();
            String paramType = getTypeDisplayName(parameter.getType());
            String description = generateParameterDescription(parameter, i);

            // 生成JSON Schema
            ObjectNode jsonSchema = null;
            try {
                jsonSchema = jsonSchemaGenerator.generateSchema(parameter.getType(), true);
            } catch (Exception e) {
                log.warn("生成JSON Schema失败: {}", parameter.getType().getName(), e);
            }

            ParameterSchema schema = ParameterSchema.builder()
                    .name(paramName)
                    .description(description)
                    .type(paramType)
                    .required(true)
                    .defaultValue("")
                    .order(i)
                    .validation(new HashMap<>())
                    .jsonSchema(jsonSchema)
                    .build();

            schemas.add(schema);
        }

        return schemas;
    }

    /**
     * 为无注解函数构建输出参数模式
     */
    private List<ParameterSchema> buildOutputSchemasWithoutAnnotation(Method method) {
        List<ParameterSchema> schemas = new ArrayList<>();

        String outputName = "result";
        String outputType = getTypeDisplayName(method.getReturnType());
        String description = "函数执行结果";

        // 生成JSON Schema
        ObjectNode jsonSchema = null;
        try {
            jsonSchema = jsonSchemaGenerator.generateSchema(method.getReturnType(), true);
        } catch (Exception e) {
            log.warn("生成JSON Schema失败: {}", method.getReturnType().getName(), e);
        }

        ParameterSchema schema = ParameterSchema.builder()
                .name(outputName)
                .description(description)
                .type(outputType)
                .required(true)
                .order(0)
                .jsonSchema(jsonSchema)
                .build();

        schemas.add(schema);
        return schemas;
    }

    /**
     * 生成默认的函数描述
     */
    private String generateDefaultDescription(Method method) {
        String methodName = method.getName();
        int paramCount = method.getParameterCount();

        if (paramCount == 0) {
            return methodName + " - 无参数函数";
        } else if (paramCount == 1) {
            return methodName + " - 单参数函数";
        } else {
            return methodName + " - " + paramCount + "参数函数";
        }
    }

    /**
     * 生成参数描述
     */
    private String generateParameterDescription(Parameter parameter, int index) {
        String paramName = parameter.getName();
        String paramType = getTypeDisplayName(parameter.getType());

        return "参数" + (index + 1) + " (" + paramType + ")";
    }

    /**
     * 获取类型的显示名称
     */
    private String getTypeDisplayName(Class<?> type) {
        if (type.isArray()) {
            return getTypeDisplayName(type.getComponentType()) + "[]";
        }

        if (type.isPrimitive()) {
            return type.getSimpleName();
        }

        // 处理泛型类型
        String typeName = type.getName();

        // 处理常见的集合类型
        if (type.getSimpleName().equals("List")) {
            return "java.util.List<Object>";
        } else if (type.getSimpleName().equals("Set")) {
            return "java.util.Set<Object>";
        } else if (type.getSimpleName().equals("Map")) {
            return "java.util.Map<String, Object>";
        } else if (type.getSimpleName().equals("Collection")) {
            return "java.util.Collection<Object>";
        }

        return typeName;
    }
}
