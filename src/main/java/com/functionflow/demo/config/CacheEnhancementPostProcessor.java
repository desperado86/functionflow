package com.functionflow.demo.config;

import com.functionflow.demo.annotation.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存增强后处理器
 * 
 * 功能包括：
 * 1. 为可缓存的函数提供自动缓存
 * 2. 缓存过期管理
 * 3. 缓存统计信息
 */
@Slf4j
@Component
public class CacheEnhancementPostProcessor implements BeanPostProcessor {

    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // 缓存统计
    private final Map<String, CacheStats> cacheStats = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 检查是否有可缓存的方法
        if (hasCacheableMethods(beanClass)) {
            log.debug("为组件添加缓存增强: {}", beanClass.getSimpleName());
            return createCacheProxy(bean, beanClass);
        }
        
        return bean;
    }

    /**
     * 检查类是否有可缓存的方法
     */
    private boolean hasCacheableMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            Function functionAnnotation = method.getAnnotation(Function.class);
            if (functionAnnotation != null && functionAnnotation.cacheable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建缓存代理
     */
    private Object createCacheProxy(Object bean, Class<?> beanClass) {
        try {
            Class<?>[] interfaces = beanClass.getInterfaces();
            
            if (interfaces.length == 0) {
                return bean;
            }
            
            return Proxy.newProxyInstance(
                    beanClass.getClassLoader(),
                    interfaces,
                    new CacheHandler(bean, beanClass)
            );
        } catch (Exception e) {
            log.warn("创建缓存代理失败: {}", beanClass.getSimpleName(), e);
            return bean;
        }
    }

    /**
     * 缓存处理器
     */
    private class CacheHandler implements java.lang.reflect.InvocationHandler {
        private final Object target;
        private final Class<?> targetClass;

        public CacheHandler(Object target, Class<?> targetClass) {
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Function functionAnnotation = method.getAnnotation(Function.class);
            
            // 检查是否是可缓存的函数方法
            if (functionAnnotation != null && functionAnnotation.cacheable()) {
                return invokeWithCache(method, args, functionAnnotation);
            } else {
                return method.invoke(target, args);
            }
        }

        /**
         * 带缓存的方法调用
         */
        private Object invokeWithCache(Method method, Object[] args, Function functionAnnotation) throws Throwable {
            String methodName = targetClass.getSimpleName() + "." + method.getName();
            String cacheKey = generateCacheKey(methodName, args);
            
            // 检查缓存
            CacheEntry cacheEntry = cache.get(cacheKey);
            if (cacheEntry != null && !cacheEntry.isExpired(functionAnnotation.cacheTime())) {
                // 缓存命中
                recordCacheHit(methodName);
                log.debug("缓存命中: {} -> {}", cacheKey, cacheEntry.value);
                return cacheEntry.value;
            }
            
            // 缓存未命中，执行原方法
            recordCacheMiss(methodName);
            
            try {
                Object result = method.invoke(target, args);
                
                // 将结果放入缓存
                cache.put(cacheKey, new CacheEntry(result, System.currentTimeMillis()));
                log.debug("缓存存储: {} -> {}", cacheKey, result);
                
                return result;
                
            } catch (Exception e) {
                log.debug("方法执行异常，不缓存结果: {}", methodName);
                throw e;
            }
        }

        /**
         * 生成缓存键
         */
        private String generateCacheKey(String methodName, Object[] args) {
            StringBuilder keyBuilder = new StringBuilder(methodName);
            if (args != null && args.length > 0) {
                keyBuilder.append(":").append(Arrays.hashCode(args));
            }
            return keyBuilder.toString();
        }

        /**
         * 记录缓存命中
         */
        private void recordCacheHit(String methodName) {
            cacheStats.compute(methodName, (key, stats) -> {
                if (stats == null) {
                    stats = new CacheStats();
                }
                stats.hitCount++;
                stats.totalCount++;
                return stats;
            });
        }

        /**
         * 记录缓存未命中
         */
        private void recordCacheMiss(String methodName) {
            cacheStats.compute(methodName, (key, stats) -> {
                if (stats == null) {
                    stats = new CacheStats();
                }
                stats.missCount++;
                stats.totalCount++;
                return stats;
            });
        }
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        final Object value;
        final long timestamp;

        CacheEntry(Object value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        boolean isExpired(int cacheTimeSeconds) {
            long cacheTimeMillis = cacheTimeSeconds * 1000L;
            return System.currentTimeMillis() - timestamp > cacheTimeMillis;
        }
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public long hitCount = 0;
        public long missCount = 0;
        public long totalCount = 0;

        public double getHitRate() {
            return totalCount > 0 ? (double) hitCount / totalCount : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hit=%d, miss=%d, total=%d, hitRate=%.2f%%}", 
                    hitCount, missCount, totalCount, getHitRate() * 100);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, CacheStats> getCacheStats() {
        return new ConcurrentHashMap<>(cacheStats);
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
        log.info("缓存已清除，共清除 {} 个条目", cache.size());
    }

    /**
     * 清除缓存统计信息
     */
    public void clearCacheStats() {
        cacheStats.clear();
        log.info("缓存统计信息已清除");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 清除过期缓存
     */
    public void evictExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        final int[] evictedCount = {0};
        
        cache.entrySet().removeIf(entry -> {
            // 这里简化处理，使用默认的缓存时间 300 秒
            boolean expired = currentTime - entry.getValue().timestamp > 300000;
            if (expired) {
                evictedCount[0]++;
            }
            return expired;
        });
        
        if (evictedCount[0] > 0) {
            log.info("清除过期缓存条目: {} 个", evictedCount[0]);
        }
    }
}
