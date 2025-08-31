package com.functionflow.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.HashMap;

/**
 * Dubbo 配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "dubbo")
public class DubboConfiguration {
    
    /**
     * 应用名称
     */
    private String applicationName = "function-flow-demo";
    
    /**
     * 默认注册中心配置
     */
    private Registry registry = new Registry();
    
    /**
     * 协议配置
     */
    private Protocol protocol = new Protocol();
    
    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();
    
    /**
     * 提供者配置
     */
    private Provider provider = new Provider();
    
    /**
     * 是否启用 Dubbo
     */
    private boolean enabled = false;
    
    /**
     * 自定义注册中心配置
     */
    private Map<String, Registry> registries = new HashMap<>();
    
    @Data
    public static class Registry {
        private String address = "zookeeper://127.0.0.1:2181";
        private String protocol = "zookeeper";
        private int timeout = 60000;
        private String username = "";
        private String password = "";
        private boolean enabled = true;
    }
    
    @Data
    public static class Protocol {
        private String name = "dubbo";
        private int port = 20880;
        private String host = "";
        private int threads = 200;
        private String serialization = "hessian2";
    }
    
    @Data
    public static class Consumer {
        private int timeout = 3000;
        private int retries = 2;
        private String loadbalance = "random";
        private boolean async = false;
        private boolean check = false;
    }
    
    @Data
    public static class Provider {
        private int timeout = 3000;
        private int retries = 2;
        private String loadbalance = "random";
        private String cluster = "failover";
        private int threads = 200;
    }
}
