package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.Functions;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标准库函数示例
 * 演示无注解函数扫描功能
 */
@Component
@Functions(name = "标准库函数", description = "Java标准库函数集合", category = "标准库", version = "1.0.0")
public class StandardLibraryFunctions {
    
    /**
     * 字符串转大写（无注解）
     */
    public String toUpperCase(String input) {
        return input.toUpperCase();
    }
    
    /**
     * 字符串转小写（无注解）
     */
    public String toLowerCase(String input) {
        return input.toLowerCase();
    }
    
    /**
     * 字符串长度（无注解）
     */
    public int length(String input) {
        return input.length();
    }
    
    /**
     * 字符串截取（无注解）
     */
    public String substring(String input, int start, int end) {
        return input.substring(start, end);
    }
    
    /**
     * 字符串替换（无注解）
     */
    public String replace(String input, String oldStr, String newStr) {
        return input.replace(oldStr, newStr);
    }
    
    /**
     * 字符串分割（无注解）
     */
    public List<String> split(String input, String delimiter) {
        return Arrays.asList(input.split(delimiter));
    }
    
    /**
     * 字符串连接（无注解）
     */
    public String join(List<String> list, String delimiter) {
        return list.stream().collect(Collectors.joining(delimiter));
    }
    
    /**
     * 数学绝对值（无注解）
     */
    public double abs(double value) {
        return Math.abs(value);
    }
    
    /**
     * 数学最大值（无注解）
     */
    public double max(double a, double b) {
        return Math.max(a, b);
    }
    
    /**
     * 数学最小值（无注解）
     */
    public double min(double a, double b) {
        return Math.min(a, b);
    }
    
    /**
     * 数学四舍五入（无注解）
     */
    public long round(double value) {
        return Math.round(value);
    }
    
    /**
     * 数学向上取整（无注解）
     */
    public double ceil(double value) {
        return Math.ceil(value);
    }
    
    /**
     * 数学向下取整（无注解）
     */
    public double floor(double value) {
        return Math.floor(value);
    }
    
    /**
     * 随机数生成（无注解）
     */
    public double random() {
        return Math.random();
    }
    
    /**
     * 静态方法示例（无注解）
     */
    public static String staticMethod(String input) {
        return "Static: " + input;
    }
    
    /**
     * 私有方法（应该被过滤掉）
     */
    private String privateMethod(String input) {
        return "Private: " + input;
    }
}
