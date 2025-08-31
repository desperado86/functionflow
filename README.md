# 函数流工作引擎 (Function Flow Work Engine)

一个功能强大的Java函数编排和工作流执行平台，支持通过反射自动发现函数、注解增强、动态UI生成、数据验证、分布式服务调用和Spring组件增强。

## 🚀 核心特性

### 1. 智能函数发现
- **反射函数发现**: 通过反射自动发现和注册函数节点
- **标准库函数支持**: 支持标准库函数（如Math类）的自动发现
- **接口函数支持**: 支持接口上的函数注解
- **无注解函数发现**: 无需特定注解即可暴露函数

### 2. 强大的注解系统
- `@Functions`: 标记函数集合并添加元数据
- `@Function`: 标记单个函数节点
- `@Input`: 描述函数输入参数
- `@Output`: 描述函数输出
- `@DubboService`: 标记分布式服务接口
- `@DubboRegistry`: 配置Dubbo注册中心信息

### 3. 分布式服务支持 (Dubbo集成)
- **动态代理**: 为Dubbo服务接口创建动态代理
- **服务发现**: 自动扫描和注册Dubbo服务
- **配置管理**: 灵活的Dubbo配置和注册中心配置
- **调用统计**: 服务调用监控和统计
- **智能回退**: 真实调用失败时自动回退到模拟模式

### 4. Spring组件增强 (BeanPostProcessor)
- **自动注册**: 自动注册函数组件到Spring容器
- **生命周期管理**: 完整的Bean生命周期管理
- **性能监控**: 函数执行性能监控
- **缓存支持**: 函数结果缓存
- **日志增强**: 详细的执行日志记录

### 5. 动态UI生成
- **JSON Schema (Draft 2020-12)**: 自动生成最新版本JSON Schema
- **前端UI自动生成**: 支持前端动态绘制UI界面
- **嵌套对象支持**: 完整支持复杂嵌套对象
- **验证消息**: JSR-303/380验证消息集成

### 6. 数据验证
- **JSR-303/380支持**: 支持`@NotNull`、`@Min`、`@Max`、`@Size`、`@Pattern`、`@Email`等验证注解
- **Hibernate Validator**: 集成Hibernate Validator
- **自动Schema生成**: 验证规则自动集成到JSON Schema
- **前后端统一验证**: 统一的验证规则

### 7. 异步执行
- **异步函数执行**: 支持函数节点的异步执行
- **超时控制**: 可配置超时时间
- **线程池管理**: 高效的线程池管理

### 8. 元数据管理
- **函数元数据**: 完整的函数元数据生成和管理
- **命名空间**: 基于类全限定名的命名空间系统
- **版本管理**: 函数版本控制
- **分类管理**: 函数分类和组织

## 📦 项目结构

```
src/main/java/com/functionflow/demo/
├── annotation/           # 注解定义
│   ├── Functions.java   # 函数集合注解
│   ├── Function.java    # 函数注解  
│   ├── Input.java       # 输入参数注解
│   ├── Output.java      # 输出注解
│   ├── DubboService.java    # Dubbo服务注解
│   └── DubboRegistry.java   # Dubbo注册中心注解
├── core/                # 核心引擎
│   ├── FunctionScanner.java         # 函数扫描器
│   ├── FunctionExecutionEngine.java # 函数执行引擎
│   ├── FunctionMetadataManager.java # 元数据管理器
│   ├── DubboProxyFactory.java       # Dubbo代理工厂
│   ├── DubboInterfaceScanner.java   # Dubbo接口扫描器
│   └── FunctionFlowBeanPostProcessor.java # Spring增强处理器
├── model/               # 数据模型
│   ├── FunctionMetadata.java    # 函数元数据
│   ├── ParameterInfo.java       # 参数信息
│   ├── ComplexObject.java       # 复杂对象示例
│   └── PersonInfo.java          # 嵌套对象示例
├── functions/           # 示例函数
│   ├── MathFunctions.java           # 数学函数
│   ├── StringFunctions.java        # 字符串函数
│   ├── ComplexObjectFunctions.java # 复杂对象函数
│   ├── StandardLibraryFunctions.java # 标准库函数
│   ├── SimpleRemoteService.java    # 远程服务接口
│   └── SimpleRemoteServiceImpl.java # 远程服务实现
├── controller/          # REST API
│   ├── FunctionController.java    # 函数API
│   ├── MetadataController.java    # 元数据API
│   ├── DubboController.java       # Dubbo管理API
│   ├── EnhancementController.java # 增强功能API
│   └── WebController.java         # Web界面
├── config/              # 配置类
│   ├── FunctionDiscoveryInitializer.java # 函数发现初始化器
│   ├── FunctionBeanDefinitionScanner.java # Bean定义扫描器
│   └── DubboConfiguration.java    # Dubbo配置
└── service/             # 服务层
    └── JsonSchemaService.java     # JSON Schema服务
```

## 🛠️ 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- Spring Boot 3.x

### 2. 启动应用

```bash
# 设置Java版本
export JAVA_HOME=/path/to/java17
export PATH=$JAVA_HOME/bin:$PATH

# 编译并启动
mvn spring-boot:run -Dmaven.compiler.source=17 -Dmaven.compiler.target=17
```

### 3. 访问Web界面

- **主界面**: http://localhost:8099
- **Dubbo管理界面**: http://localhost:8099/dubbo-manager.html
- **健康检查**: http://localhost:8099/actuator/health

### 4. 核心API接口

#### 函数管理
```bash
# 获取所有函数
GET /api/functions

# 执行函数
POST /api/functions/{functionId}/execute
Content-Type: application/json
{
  "name": "World"
}

# 获取函数Schema
GET /api/functions/{functionId}/schema
```

#### 元数据管理
```bash
# 获取所有元数据
GET /api/metadata

# 刷新元数据
POST /api/metadata/refresh

# 获取统计信息
GET /api/metadata/stats
```

#### Dubbo服务管理
```bash
# 获取Dubbo状态
GET /api/dubbo/status

# 获取服务概览
GET /api/dubbo/overview

# 获取服务统计
GET /api/dubbo/service-stats

# 清除缓存
DELETE /api/dubbo/caches
```

#### 增强功能管理
```bash
# 获取增强状态
GET /api/enhancement/status

# 获取性能统计
GET /api/enhancement/performance-stats

# 清除缓存
DELETE /api/enhancement/caches
```

## 📝 使用示例

### 1. 创建基础函数

```java
@Component
@Functions(
    name = "自定义函数集",
    description = "包含自定义函数的集合",
    category = "自定义"
)
public class MyFunctions {
    
    @Function(
        name = "自定义计算",
        description = "这是一个自定义计算函数",
        category = "数学计算"
    )
    @Output(name = "result", description = "计算结果", type = "Double")
    public double myFunction(
            @Input(name = "x", description = "输入值", type = "Double", required = true) 
            @NotNull @Min(0) double x) {
        return x * x + 2 * x + 1;
    }
}
```

### 2. 创建复杂对象函数

```java
@Function(
    name = "创建用户信息",
    description = "创建包含验证的用户信息对象"
)
@Output(name = "userInfo", description = "创建的用户信息", type = "PersonInfo")
public PersonInfo createUserInfo(
        @Input(name = "name", description = "用户姓名", required = true)
        @NotNull @Size(min = 2, max = 50, message = "姓名长度必须在2-50个字符之间") 
        String name,
        
        @Input(name = "age", description = "用户年龄", required = true)
        @NotNull @Min(value = 0, message = "年龄不能为负数") 
        @Max(value = 150, message = "年龄不能超过150岁") 
        Integer age,
        
        @Input(name = "email", description = "电子邮箱", required = true)
        @NotNull @Email(message = "请输入有效的电子邮箱") 
        String email) {
    
    return PersonInfo.builder()
            .name(name)
            .age(age)
            .email(email)
            .createdAt(LocalDateTime.now())
            .build();
}
```

### 3. 创建Dubbo服务接口

```java
@DubboService(
    interfaceName = "com.example.MyRemoteService",
    version = "1.0.0",
    group = "production",
    timeout = 5000,
    description = "远程计算服务"
)
@DubboRegistry(
    protocol = "zookeeper",
    address = "127.0.0.1:2181"
)
@Functions(
    name = "远程服务",
    description = "提供远程调用功能",
    category = "分布式服务"
)
public interface MyRemoteService {

    @Function(
        name = "远程计算",
        description = "通过Dubbo进行远程计算"
    )
    @Output(name = "result", description = "计算结果", type = "Double")
    double remoteCalculate(
            @Input(name = "a", description = "第一个数", required = true) double a,
            @Input(name = "b", description = "第二个数", required = true) double b,
            @Input(name = "operation", description = "运算符", required = true) String operation);
}
```

### 4. 实现本地服务（开发环境）

```java
@Component
@Functions(
    name = "远程服务本地实现",
    description = "远程服务的本地实现",
    category = "分布式服务"
)
public class MyRemoteServiceImpl implements MyRemoteService {

    @Override
    public double remoteCalculate(double a, double b, String operation) {
        switch (operation.toLowerCase()) {
            case "add": return a + b;
            case "subtract": return a - b;
            case "multiply": return a * b;
            case "divide": 
                if (b == 0) throw new IllegalArgumentException("除数不能为零");
                return a / b;
            default: 
                throw new IllegalArgumentException("不支持的运算: " + operation);
        }
    }
}
```

## 🔧 配置

### 应用配置 (application.properties)

```properties
# 服务端口
server.port=8099

# 函数扫描包
functionflow.scan.packages=com.functionflow.demo.functions

# Dubbo配置
dubbo.enabled=false
dubbo.application-name=function-flow-demo
dubbo.registry.address=zookeeper://127.0.0.1:2181
dubbo.registry.protocol=zookeeper
dubbo.registry.timeout=60000
dubbo.protocol.name=dubbo
dubbo.protocol.port=20880
dubbo.consumer.timeout=3000
dubbo.consumer.retries=2

# Spring组件增强
functionflow.enhancement.enabled=true
functionflow.enhancement.performance-monitoring=true
functionflow.enhancement.caching=true
functionflow.enhancement.logging=true

# 元数据管理
functionflow.metadata.auto-refresh=true
functionflow.metadata.cache-enabled=true
```

### Dubbo集成配置

```java
@Configuration
@ConfigurationProperties(prefix = "dubbo")
@Builder
@Data
public class DubboConfiguration {
    
    @Builder.Default
    private boolean enabled = false;
    
    @Builder.Default
    private String applicationName = "function-flow-demo";
    
    @Builder.Default
    private Registry registry = Registry.builder().build();
    
    @Builder.Default
    private Protocol protocol = Protocol.builder().build();
    
    // ... 其他配置
}
```

## 🧪 测试示例

### 基础函数测试
```bash
# 测试数学函数
curl -X POST "http://localhost:8099/api/functions/com.functionflow.demo.functions.MathFunctions.add/execute" \
  -H "Content-Type: application/json" \
  -d '{"a": 10, "b": 5}'
# 返回: 15.0

# 测试字符串函数
curl -X POST "http://localhost:8099/api/functions/com.functionflow.demo.functions.StringFunctions.concat/execute" \
  -H "Content-Type: application/json" \
  -d '{"str1": "Hello", "str2": "World"}'
# 返回: "HelloWorld"
```

### 复杂对象测试
```bash
# 创建复杂对象
curl -X POST "http://localhost:8099/api/functions/com.functionflow.demo.functions.ComplexObjectFunctions.createComplexObject/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试对象",
    "value": 100,
    "tags": ["tag1", "tag2"]
  }'
```

### 远程服务测试
```bash
# 测试远程服务实现
curl -X POST "http://localhost:8099/api/functions/com.functionflow.demo.functions.SimpleRemoteServiceImpl.sayHello/execute" \
  -H "Content-Type: application/json" \
  -d '{"name": "世界"}'
# 返回: "Hello, 世界! (来自本地服务)"
```

## 📊 功能特性对比

| 特性 | 支持状态 | 说明 |
|------|----------|------|
| 反射函数发现 | ✅ | 自动发现和注册函数 |
| 注解增强 | ✅ | 完整的元数据支持 |
| JSON Schema生成 | ✅ | Draft 2020-12标准 |
| 数据验证 | ✅ | JSR-303/380 + Hibernate Validator |
| 异步执行 | ✅ | 线程池管理 |
| Dubbo集成 | ✅ | 完整的分布式服务支持 |
| Spring增强 | ✅ | BeanPostProcessor增强 |
| 接口函数支持 | ✅ | 支持接口上的函数注解 |
| 复杂对象支持 | ✅ | 嵌套对象和验证 |
| 标准库支持 | ✅ | Math、String等标准库 |
| 元数据管理 | ✅ | 完整的元数据系统 |
| 性能监控 | ✅ | 执行时间和调用统计 |
| 缓存支持 | ✅ | 多级缓存机制 |
| 可视化界面 | ✅ | Web管理界面 |

## 🎯 技术架构

### 核心组件
- **FunctionScanner**: 负责扫描和发现函数
- **FunctionExecutionEngine**: 负责执行函数调用
- **DubboProxyFactory**: 创建和管理Dubbo服务代理
- **FunctionMetadataManager**: 管理函数元数据
- **JsonSchemaService**: 生成JSON Schema
- **FunctionFlowBeanPostProcessor**: Spring组件增强

### 技术栈
- **Spring Boot 3.5.5**: 基础框架
- **Java 17**: 编程语言
- **Apache Dubbo 3.2.12**: 分布式服务框架
- **Hibernate Validator**: 数据验证
- **Jackson**: JSON处理
- **Lombok**: 代码简化
- **Maven**: 构建工具

## 🚧 扩展计划

### 短期计划
- [ ] 支持更多标准库函数
- [ ] 增强可视化工作流设计器
- [ ] 添加函数执行监控和告警
- [ ] 支持函数链式调用
- [ ] 增加更多Dubbo配置选项

### 中期计划
- [ ] 支持分布式执行
- [ ] 添加函数版本管理
- [ ] 支持条件分支和循环
- [ ] 集成消息队列
- [ ] 添加函数市场

### 长期计划
- [ ] 支持多语言函数（Python、Node.js等）
- [ ] 机器学习模型集成
- [ ] 云原生部署支持
- [ ] 可视化调试工具
- [ ] 企业级权限管理

## 🔍 监控和运维

### 健康检查
```bash
# 应用健康状态
GET /actuator/health

# Dubbo服务状态
GET /api/dubbo/status

# 增强功能状态
GET /api/enhancement/status
```

### 性能监控
```bash
# 函数执行统计
GET /api/enhancement/performance-stats

# Dubbo服务调用统计
GET /api/dubbo/service-stats

# 元数据统计
GET /api/metadata/stats
```

### 缓存管理
```bash
# 清除所有缓存
DELETE /api/enhancement/caches
DELETE /api/dubbo/caches

# 刷新元数据
POST /api/metadata/refresh
```

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交Issue和Pull Request！

### 贡献指南
1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📞 联系方式

如有问题，请提交Issue或联系开发团队。

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者！

---

**函数流工作引擎** - 让函数编排和分布式服务调用变得简单高效！ 🚀