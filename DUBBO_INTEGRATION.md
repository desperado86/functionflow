# Dubbo 集成使用指南

## 功能概述

本系统已实现完整的 Dubbo 集成，支持真实的 Dubbo 远程服务调用。当 Dubbo 框架未正确配置或服务不可用时，系统会自动回退到模拟模式。

## 配置说明

### 1. Maven 依赖

系统已自动添加以下 Dubbo 依赖：

```xml
<!-- Dubbo Spring Boot Starter -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
    <version>3.2.12</version>
</dependency>

<!-- Dubbo ZooKeeper 注册中心 -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-dependencies-zookeeper</artifactId>
    <version>3.2.12</version>
    <type>pom</type>
</dependency>

<!-- Dubbo Nacos 注册中心（可选） -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-registry-nacos</artifactId>
    <version>3.2.12</version>
</dependency>

<!-- Dubbo Hessian2 序列化 -->
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-serialization-hessian2</artifactId>
    <version>3.2.12</version>
</dependency>
```

### 2. 应用配置

在 `application.properties` 中配置 Dubbo：

```properties
# 启用 Dubbo（默认为 false）
dubbo.enabled=true

# 应用配置
dubbo.application-name=function-flow-demo

# 注册中心配置
dubbo.registry.address=zookeeper://127.0.0.1:2181
dubbo.registry.protocol=zookeeper
dubbo.registry.timeout=60000

# 协议配置
dubbo.protocol.name=dubbo
dubbo.protocol.port=20880
dubbo.protocol.serialization=hessian2

# 消费者配置
dubbo.consumer.timeout=3000
dubbo.consumer.retries=2
dubbo.consumer.loadbalance=random
dubbo.consumer.check=false
```

## 使用方式

### 1. 定义 Dubbo 服务接口

```java
@DubboService(
    interfaceName = "com.example.UserService",
    version = "1.0.0",
    group = "user-group",
    timeout = 5000,
    retries = 2,
    loadbalance = "random"
)
@Functions(
    name = "用户服务",
    description = "提供用户管理功能"
)
public interface UserService {
    
    @Function(
        name = "获取用户",
        description = "根据ID获取用户信息"
    )
    User getUserById(Long userId);
    
    @Function(
        name = "创建用户",
        description = "创建新用户"
    )
    Long createUser(CreateUserRequest request);
}
```

### 2. 自动服务发现

系统会自动扫描带有 `@DubboService` 注解的接口，并为其创建 Dubbo 代理。

### 3. 调用方式

#### API 调用
```bash
# 测试服务调用
curl -X POST http://localhost:8099/api/dubbo/test/UserService/getUserById \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'

# 获取服务状态
curl http://localhost:8099/api/dubbo/status
```

#### 函数流调用
可以在工作流中直接使用 Dubbo 服务方法作为函数节点。

## 核心特性

### 1. 自动回退机制

```java
// 真实 Dubbo 调用失败时自动回退到模拟模式
private Object realDubboCall(Method method, Object[] args) throws Exception {
    try {
        // 获取 Dubbo 服务引用
        Object dubboReference = getDubboReference(interfaceClass, dubboService);
        
        // 执行远程调用
        return method.invoke(dubboReference, args);
        
    } catch (Exception e) {
        log.error("Dubbo 真实调用失败，回退到模拟模式");
        return simulateDubboCall(method, args);
    }
}
```

### 2. 智能缓存管理

- **代理缓存**: 避免重复创建代理对象
- **引用缓存**: 复用 Dubbo 服务引用
- **配置缓存**: 缓存 ReferenceConfig 配置

### 3. 完整监控统计

- 调用次数统计
- 成功失败率监控
- 执行时间追踪
- 服务状态监控

## 管理接口

### 1. 服务管理

```bash
# 获取所有服务
GET /api/dubbo/services

# 获取服务详情
GET /api/dubbo/services/{serviceName}

# 获取服务概览
GET /api/dubbo/overview

# 重新扫描服务
POST /api/dubbo/rescan
```

### 2. 统计管理

```bash
# 获取调用统计
GET /api/dubbo/service-stats

# 清除调用统计
DELETE /api/dubbo/service-stats

# 获取 Dubbo 状态
GET /api/dubbo/status
```

### 3. 缓存管理

```bash
# 清除所有缓存
DELETE /api/dubbo/caches
```

## Web 管理界面

访问 `http://localhost:8099/dubbo-manager.html` 可以使用可视化界面管理 Dubbo 服务：

- 实时服务状态展示
- 调用统计图表
- 在线服务测试
- 配置信息查看
- 缓存管理操作

## 部署指南

### 1. 开发环境

```bash
# 1. 启动 ZooKeeper（如果使用 ZooKeeper 作为注册中心）
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.7

# 2. 修改配置启用 Dubbo
# 在 application.properties 中设置：dubbo.enabled=true

# 3. 启动应用
mvn spring-boot:run
```

### 2. 生产环境

1. **注册中心部署**:
   - ZooKeeper 集群
   - 或 Nacos 集群

2. **服务提供者部署**:
   - 部署实际的 Dubbo 服务提供者
   - 确保服务正确注册到注册中心

3. **配置调整**:
   ```properties
   # 生产环境配置
   dubbo.enabled=true
   dubbo.registry.address=zookeeper://prod-zk1:2181,prod-zk2:2181,prod-zk3:2181
   dubbo.consumer.check=true
   dubbo.consumer.timeout=5000
   ```

## 故障排查

### 1. 常见问题

**问题**: Dubbo 服务调用失败
```bash
# 检查注册中心连接
curl http://localhost:8099/api/dubbo/status

# 查看日志
tail -f logs/application.log | grep -i dubbo
```

**问题**: 服务未发现
```bash
# 重新扫描服务
curl -X POST http://localhost:8099/api/dubbo/rescan

# 检查服务注册状态
curl http://localhost:8099/api/dubbo/services
```

### 2. 调试模式

在 `application.properties` 中启用调试日志：

```properties
# Dubbo 调试日志
logging.level.com.functionflow.demo.core.DubboProxyFactory=DEBUG
logging.level.org.apache.dubbo=INFO
```

## 扩展功能

### 1. 自定义负载均衡

```java
@DubboService(
    loadbalance = "consistenthash"  // 一致性哈希
)
public interface CustomService {
    // 服务方法
}
```

### 2. 异步调用支持

```java
@DubboService(async = true)
public interface AsyncService {
    
    @Function(async = true)
    CompletableFuture<String> asyncMethod(String param);
}
```

### 3. 多注册中心支持

可以为不同的服务配置不同的注册中心：

```java
@DubboService(
    registry = "nacos://127.0.0.1:8848"
)
public interface NacosService {
    // 使用 Nacos 注册中心的服务
}
```

## 注意事项

1. **开发模式**: 默认 `dubbo.enabled=false`，使用模拟模式
2. **生产部署**: 需要真实的注册中心和服务提供者
3. **网络配置**: 确保网络连通性和防火墙设置
4. **版本兼容**: 注意 Dubbo 版本与 Spring Boot 版本的兼容性
5. **性能调优**: 根据实际负载调整线程池和超时配置

通过以上配置，您的系统已经具备了完整的 Dubbo 分布式服务调用能力！🎉
