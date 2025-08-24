package com.functionflow.demo.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * JSON Schema生成器
 * 用于为复杂对象生成JSON Schema，支持Draft 2020-12规范
 */
@Slf4j
@Component
public class JsonSchemaGenerator {
    
    private final ObjectMapper objectMapper;
    private final Map<String, ObjectNode> schemaCache = new HashMap<>();
    
    public JsonSchemaGenerator() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 为类生成JSON Schema（默认使用完整Schema）
     */
    public ObjectNode generateSchema(Class<?> clazz) {
        return generateSchema(clazz, true);
    }
    
    /**
     * 为类生成JSON Schema
     * @param clazz 要生成Schema的类
     * @param useFullSchema 是否使用完整的JSON Schema（true）还是简单类型描述（false）
     */
    public ObjectNode generateSchema(Class<?> clazz, boolean useFullSchema) {
        String cacheKey = clazz.getName() + "_" + useFullSchema;
        if (schemaCache.containsKey(cacheKey)) {
            return schemaCache.get(cacheKey);
        }
        
        ObjectNode schema = objectMapper.createObjectNode();
        
        if (useFullSchema) {
            // 使用Draft 2020-12规范
            schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
            
            if (clazz.isPrimitive() || isBasicType(clazz)) {
                // 基本类型使用简单Schema
                generateBasicTypeSchema(schema, clazz);
            } else {
                // 复杂对象使用完整Schema
                schema.put("type", "object");
                schema.put("title", clazz.getSimpleName());
                schema.put("description", getClassDescription(clazz));
                
                ObjectNode properties = objectMapper.createObjectNode();
                com.fasterxml.jackson.databind.node.ArrayNode required = objectMapper.createArrayNode();
                
                // 获取所有字段
                List<Field> fields = getAllFields(clazz);
                
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    
                    String fieldName = field.getName();
                    Class<?> fieldType = field.getType();
                    
                    // 生成字段的Schema
                    ObjectNode fieldSchema = generateFieldSchema(field, useFullSchema);
                    properties.set(fieldName, fieldSchema);
                    
                    // 检查是否必需
                    if (isRequiredField(field)) {
                        required.add(fieldName);
                    }
                }
                
                schema.set("properties", properties);
                if (required.size() > 0) {
                    schema.set("required", required);
                }
            }
        } else {
            // 简单类型描述
            generateSimpleTypeSchema(schema, clazz);
        }
        
        // 缓存Schema
        schemaCache.put(cacheKey, schema);
        
        return schema;
    }
    
    /**
     * 生成基本类型的Schema
     */
    private void generateBasicTypeSchema(ObjectNode schema, Class<?> clazz) {
        if (clazz == String.class) {
            schema.put("type", "string");
            schema.put("title", "String");
            schema.put("description", "字符串类型");
        } else if (clazz == Integer.class || clazz == int.class) {
            schema.put("type", "integer");
            schema.put("title", "Integer");
            schema.put("description", "整数类型");
        } else if (clazz == Long.class || clazz == long.class) {
            schema.put("type", "integer");
            schema.put("format", "int64");
            schema.put("title", "Long");
            schema.put("description", "长整数类型");
        } else if (clazz == Double.class || clazz == double.class) {
            schema.put("type", "number");
            schema.put("title", "Double");
            schema.put("description", "双精度浮点数类型");
        } else if (clazz == Float.class || clazz == float.class) {
            schema.put("type", "number");
            schema.put("format", "float");
            schema.put("title", "Float");
            schema.put("description", "单精度浮点数类型");
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            schema.put("type", "boolean");
            schema.put("title", "Boolean");
            schema.put("description", "布尔类型");
        } else if (clazz == Byte.class || clazz == byte.class) {
            schema.put("type", "integer");
            schema.put("minimum", -128);
            schema.put("maximum", 127);
            schema.put("title", "Byte");
            schema.put("description", "字节类型");
        } else if (clazz == Short.class || clazz == short.class) {
            schema.put("type", "integer");
            schema.put("minimum", -32768);
            schema.put("maximum", 32767);
            schema.put("title", "Short");
            schema.put("description", "短整数类型");
        } else if (clazz == Character.class || clazz == char.class) {
            schema.put("type", "string");
            schema.put("minLength", 1);
            schema.put("maxLength", 1);
            schema.put("title", "Character");
            schema.put("description", "字符类型");
        } else {
            // 默认处理
            schema.put("type", "object");
            schema.put("title", clazz.getSimpleName());
            schema.put("description", clazz.getSimpleName() + " 类型");
        }
    }
    
    /**
     * 生成简单类型描述
     */
    private void generateSimpleTypeSchema(ObjectNode schema, Class<?> clazz) {
        if (clazz == String.class) {
            schema.put("type", "string");
            schema.put("javaType", "java.lang.String");
        } else if (clazz == Integer.class || clazz == int.class) {
            schema.put("type", "integer");
            schema.put("javaType", "java.lang.Integer");
        } else if (clazz == Long.class || clazz == long.class) {
            schema.put("type", "integer");
            schema.put("javaType", "java.lang.Long");
        } else if (clazz == Double.class || clazz == double.class) {
            schema.put("type", "number");
            schema.put("javaType", "java.lang.Double");
        } else if (clazz == Float.class || clazz == float.class) {
            schema.put("type", "number");
            schema.put("javaType", "java.lang.Float");
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            schema.put("type", "boolean");
            schema.put("javaType", "java.lang.Boolean");
        } else if (clazz == Byte.class || clazz == byte.class) {
            schema.put("type", "integer");
            schema.put("javaType", "java.lang.Byte");
        } else if (clazz == Short.class || clazz == short.class) {
            schema.put("type", "integer");
            schema.put("javaType", "java.lang.Short");
        } else if (clazz == Character.class || clazz == char.class) {
            schema.put("type", "string");
            schema.put("javaType", "java.lang.Character");
        } else {
            schema.put("type", "object");
            schema.put("javaType", clazz.getName());
        }
    }
    
    /**
     * 检查是否为基本类型
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz == String.class || 
               clazz == Integer.class || clazz == int.class ||
               clazz == Long.class || clazz == long.class ||
               clazz == Double.class || clazz == double.class ||
               clazz == Float.class || clazz == float.class ||
               clazz == Boolean.class || clazz == boolean.class ||
               clazz == Byte.class || clazz == byte.class ||
               clazz == Short.class || clazz == short.class ||
               clazz == Character.class || clazz == char.class;
    }
    
    /**
     * 为字段生成Schema
     */
    private ObjectNode generateFieldSchema(Field field) {
        return generateFieldSchema(field, true);
    }
    
    /**
     * 为字段生成Schema
     */
    private ObjectNode generateFieldSchema(Field field, boolean useFullSchema) {
        ObjectNode schema = objectMapper.createObjectNode();
        Class<?> fieldType = field.getType();
        
        // 设置字段描述
        String description = getFieldDescription(field);
        if (description != null && !description.isEmpty()) {
            schema.put("description", description);
        }
        
        // 处理基本类型
        if (fieldType == String.class) {
            schema.put("type", "string");
            addStringValidation(schema, field);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            schema.put("type", "integer");
            addNumberValidation(schema, field);
        } else if (fieldType == Long.class || fieldType == long.class) {
            schema.put("type", "integer");
            schema.put("format", "int64");
            addNumberValidation(schema, field);
        } else if (fieldType == Double.class || fieldType == double.class) {
            schema.put("type", "number");
            addNumberValidation(schema, field);
        } else if (fieldType == Float.class || fieldType == float.class) {
            schema.put("type", "number");
            schema.put("format", "float");
            addNumberValidation(schema, field);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            schema.put("type", "boolean");
        } else if (fieldType == Byte.class || fieldType == byte.class) {
            schema.put("type", "integer");
            schema.put("minimum", -128);
            schema.put("maximum", 127);
        } else if (fieldType == Short.class || fieldType == short.class) {
            schema.put("type", "integer");
            schema.put("minimum", -32768);
            schema.put("maximum", 32767);
        } else if (fieldType == Character.class || fieldType == char.class) {
            schema.put("type", "string");
            schema.put("minLength", 1);
            schema.put("maxLength", 1);
        } else if (fieldType.isArray()) {
            schema.put("type", "array");
            ObjectNode itemsSchema = generateSchemaForType(field.getType().getComponentType(), useFullSchema);
            schema.set("items", itemsSchema);
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            schema.put("type", "array");
            // 尝试获取泛型类型
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] typeArguments = paramType.getActualTypeArguments();
                if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                    Class<?> elementType = (Class<?>) typeArguments[0];
                    ObjectNode itemsSchema = generateSchemaForType(elementType, useFullSchema);
                    schema.set("items", itemsSchema);
                } else {
                    schema.set("items", objectMapper.createObjectNode().put("type", "object"));
                }
            } else {
                schema.set("items", objectMapper.createObjectNode().put("type", "object"));
            }
        } else if (Map.class.isAssignableFrom(fieldType)) {
            schema.put("type", "object");
            schema.set("additionalProperties", objectMapper.createObjectNode().put("type", "object"));
        } else if (fieldType.isEnum()) {
            schema.put("type", "string");
            com.fasterxml.jackson.databind.node.ArrayNode enumValues = objectMapper.createArrayNode();
            for (Object enumConstant : fieldType.getEnumConstants()) {
                enumValues.add(enumConstant.toString());
            }
            schema.set("enum", enumValues);
        } else if (!fieldType.isPrimitive()) {
            // 复杂对象，递归生成Schema
            schema.put("type", "object");
            ObjectNode refSchema = generateSchema(fieldType, useFullSchema);
            schema.set("properties", refSchema.get("properties"));
            if (refSchema.has("required")) {
                schema.set("required", refSchema.get("required"));
            }
        }
        
        return schema;
    }
    
    /**
     * 为类型生成Schema（用于数组元素等）
     */
    private ObjectNode generateSchemaForType(Class<?> type) {
        return generateSchemaForType(type, true);
    }
    
    /**
     * 为类型生成Schema（用于数组元素等）
     */
    private ObjectNode generateSchemaForType(Class<?> type, boolean useFullSchema) {
        if (useFullSchema) {
            return generateSchema(type, useFullSchema);
        } else {
            ObjectNode schema = objectMapper.createObjectNode();
            generateSimpleTypeSchema(schema, type);
            return schema;
        }
    }
    
    /**
     * 添加字符串验证规则
     */
    private void addStringValidation(ObjectNode schema, Field field) {
        // 检查验证注解
        if (field.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class)) {
            schema.put("minLength", 1);
        }
        
        if (field.isAnnotationPresent(jakarta.validation.constraints.Size.class)) {
            jakarta.validation.constraints.Size size = field.getAnnotation(jakarta.validation.constraints.Size.class);
            if (size.min() > 0) {
                schema.put("minLength", size.min());
            }
            if (size.max() < Integer.MAX_VALUE) {
                schema.put("maxLength", size.max());
            }
        }
        
        if (field.isAnnotationPresent(jakarta.validation.constraints.Pattern.class)) {
            jakarta.validation.constraints.Pattern pattern = field.getAnnotation(jakarta.validation.constraints.Pattern.class);
            schema.put("pattern", pattern.regexp());
        }
        
        if (field.isAnnotationPresent(jakarta.validation.constraints.Email.class)) {
            schema.put("format", "email");
        }
    }
    
    /**
     * 添加数字验证规则
     */
    private void addNumberValidation(ObjectNode schema, Field field) {
        if (field.isAnnotationPresent(jakarta.validation.constraints.Min.class)) {
            jakarta.validation.constraints.Min min = field.getAnnotation(jakarta.validation.constraints.Min.class);
            schema.put("minimum", min.value());
        }
        
        if (field.isAnnotationPresent(jakarta.validation.constraints.Max.class)) {
            jakarta.validation.constraints.Max max = field.getAnnotation(jakarta.validation.constraints.Max.class);
            schema.put("maximum", max.value());
        }
        
        if (field.isAnnotationPresent(jakarta.validation.constraints.DecimalMin.class)) {
            jakarta.validation.constraints.DecimalMin decimalMin = field.getAnnotation(jakarta.validation.constraints.DecimalMin.class);
            schema.put("minimum", decimalMin.value());
        }
        
        if (field.isAnnotationPresent(jakarta.validation.constraints.DecimalMax.class)) {
            jakarta.validation.constraints.DecimalMax decimalMax = field.getAnnotation(jakarta.validation.constraints.DecimalMax.class);
            schema.put("maximum", decimalMax.value());
        }
    }
    
    /**
     * 获取所有字段（包括继承的字段）
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        
        return fields;
    }
    
    /**
     * 检查字段是否必需
     */
    private boolean isRequiredField(Field field) {
        return field.isAnnotationPresent(jakarta.validation.constraints.NotNull.class) ||
               field.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class) ||
               field.isAnnotationPresent(jakarta.validation.constraints.NotEmpty.class);
    }
    
    /**
     * 获取类描述
     */
    private String getClassDescription(Class<?> clazz) {
        // 可以扩展为从注解或其他地方获取描述
        return clazz.getSimpleName() + " 对象";
    }
    
    /**
     * 获取字段描述
     */
    private String getFieldDescription(Field field) {
        // 可以扩展为从注解或其他地方获取描述
        return field.getName() + " 字段";
    }
    
    /**
     * 验证JSON数据是否符合Schema
     */
    public boolean validateJson(JsonNode jsonData, ObjectNode schema) {
        try {
            // 这里可以集成JSON Schema验证库，如json-schema-validator
            // 目前返回true，实际项目中应该使用专门的验证库
            return true;
        } catch (Exception e) {
            log.error("JSON Schema验证失败", e);
            return false;
        }
    }
    
    /**
     * 获取缓存的Schema
     */
    public ObjectNode getCachedSchema(Class<?> clazz) {
        return schemaCache.get(clazz.getName() + "_true");
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        schemaCache.clear();
    }
}
