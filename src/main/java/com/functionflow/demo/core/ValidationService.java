package com.functionflow.demo.core;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 验证服务
 * 使用 Hibernate Validator 实现 JSR-303/JSR-380 规范
 */
@Slf4j
@Service
public class ValidationService {
    
    private final Validator validator;
    
    public ValidationService() {
        // 使用 Hibernate Validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }
    
    /**
     * 验证对象
     */
    public <T> Set<ConstraintViolation<T>> validate(T object) {
        return validator.validate(object);
    }
    
    /**
     * 验证对象的特定属性
     */
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName) {
        return validator.validateProperty(object, propertyName);
    }
    
    /**
     * 验证值
     */
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value) {
        return validator.validateValue(beanType, propertyName, value);
    }
    
    /**
     * 验证并返回错误信息
     */
    public <T> Map<String, String> validateAndGetErrors(T object) {
        Set<ConstraintViolation<T>> violations = validate(object);
        Map<String, String> errors = new HashMap<>();
        
        for (ConstraintViolation<T> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }
        
        return errors;
    }
    
    /**
     * 检查对象是否有效
     */
    public <T> boolean isValid(T object) {
        Set<ConstraintViolation<T>> violations = validate(object);
        return violations.isEmpty();
    }
    
    /**
     * 获取验证器实例
     */
    public Validator getValidator() {
        return validator;
    }
}
