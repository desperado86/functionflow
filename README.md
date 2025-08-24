# 函数流工作引擎 (Function Flow Work Engine)

一个功能强大的Java函数编排和工作流执行平台，支持通过反射自动发现函数、注解增强、动态UI生成、数据验证、异步执行和工作流模块化。

## 🚀 核心特性

### 1. 反射函数发现
- 通过反射自动发现和注册函数节点
- 支持标准库函数（如Math类）的自动发现
- 无需特定注解即可暴露函数

### 2. 注解增强
- `@FunctionNode`: 标记函数节点并添加元数据
- `@FunctionInput`: 描述函数输入参数
- `@FunctionOutput`: 描述函数输出
- 支持名称、描述、分类、版本等元数据

### 3. 动态UI生成
- 自动生成JSON Schema描述文件
- 支持前端动态绘制UI界面
- 提供完整的参数类型和验证信息

### 4. 数据验证
- 支持`@NotNull`、`@Min`、`@Max`、`@Size`、`@Pattern`、`@Email`等验证注解
- 自动生成验证Schema
- 前后端统一验证规则

### 5. 异步执行
- 支持函数节点的异步执行
- 可配置超时时间
- 线程池管理

### 6. 工作流模块化
- 支持工作流封装为模块
- 模块可被其他工作流引用
- 完整的输入输出定义

## 📦 项目结构

```
src/main/java/com/functionflow/demo/
├── annotation/           # 注解定义
│   ├── FunctionNode.java
│   ├── FunctionInput.java
│   └── FunctionOutput.java
├── core/                # 核心引擎
│   ├── FunctionDiscoveryService.java
│   ├── FunctionExecutionEngine.java
│   └── WorkflowExecutionEngine.java
├── model/               # 数据模型
│   ├── FunctionSchema.java
│   ├── ParameterSchema.java
│   ├── Workflow.java
│   └── WorkflowNode.java
├── functions/           # 示例函数
│   ├── MathFunctions.java
│   └── StringFunctions.java
├── controller/          # REST API
│   ├── FunctionController.java
│   ├── WorkflowController.java
│   └── WebController.java
├── config/              # 配置类
│   ├── AsyncConfig.java
│   └── FunctionDiscoveryInitializer.java
└── example/             # 示例工作流
    └── ExampleWorkflow.java
```

## 🛠️ 快速开始

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 访问Web界面

打开浏览器访问: http://localhost:8080

### 3. API接口

#### 获取所有函数
```bash
GET /api/functions
```

#### 执行函数
```bash
POST /api/functions/{functionId}/execute
Content-Type: application/json

{
  "a": 5,
  "b": 3
}
```

#### 获取函数Schema
```bash
GET /api/functions/{functionId}/schema
```

## 📝 使用示例

### 1. 创建函数

```java
@Component
public class MyFunctions {
    
    @FunctionNode(
        name = "自定义函数",
        description = "这是一个自定义函数",
        category = "自定义",
        version = "1.0.0"
    )
    @FunctionOutput(name = "result", description = "计算结果", type = "Double")
    public double myFunction(
            @FunctionInput(name = "x", description = "输入值", type = "Double", required = true) 
            @NotNull @Min(0) double x) {
        return x * x + 2 * x + 1;
    }
}
```

### 2. 创建工作流

```java
Workflow workflow = Workflow.builder()
    .id("my-workflow")
    .name("我的工作流")
    .description("一个示例工作流")
    .version("1.0.0")
    .nodes(Arrays.asList(
        WorkflowNode.builder()
            .id("node1")
            .type("function")
            .name("计算节点")
            .functionId("MyFunctions.myFunction")
            .position(WorkflowNode.Position.builder().x(100).y(100).build())
            .build()
    ))
    .build();
```

### 3. 注册和执行工作流

```java
// 注册工作流
workflowExecutionEngine.registerWorkflow(workflow);

// 执行工作流
Map<String, Object> inputs = Map.of("x", 5.0);
Map<String, Object> result = workflowExecutionEngine.executeWorkflow("my-workflow", inputs);
```

## 🔧 配置

### 异步执行配置

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FunctionFlow-");
        executor.initialize();
        return executor;
    }
}
```

## 🧪 测试

运行测试：

```bash
mvn test
```

测试覆盖：
- 函数发现和注册
- 函数执行
- 参数验证
- 异步执行
- 工作流执行

## 📊 功能特性对比

| 特性 | 支持状态 | 说明 |
|------|----------|------|
| 反射函数发现 | ✅ | 自动发现和注册函数 |
| 注解增强 | ✅ | 完整的元数据支持 |
| JSON Schema生成 | ✅ | 动态UI支持 |
| 数据验证 | ✅ | 前后端统一验证 |
| 异步执行 | ✅ | 线程池管理 |
| 工作流模块化 | ✅ | 模块复用 |
| 标准库支持 | 🔄 | 部分支持，可扩展 |
| 可视化设计器 | 🔄 | 基础支持，可扩展 |

## 🚧 扩展计划

- [ ] 支持更多标准库函数
- [ ] 增强可视化工作流设计器
- [ ] 添加函数执行监控和日志
- [ ] 支持分布式执行
- [ ] 添加函数版本管理
- [ ] 支持条件分支和循环

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📞 联系方式

如有问题，请提交Issue或联系开发团队。
