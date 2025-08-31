package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.annotation.Function;
import org.springframework.stereotype.Component;

/**
 * 数学函数示例
 */
@Component
@Functions(name = "数学函数", description = "基础数学运算函数集合", category = "数学运算", version = "1.0.0")
public class MathFunctions implements FunctionService {

    @Function(name = "加法", description = "计算两个数的和", cacheable = true, cacheTime = 60)
    public double add(double a, double b) {
        return a + b;
    }

    @Function(name = "减法", description = "计算两个数的差")
    public double subtract(double a, double b) {
        return a - b;
    }

    @Function(name = "乘法", description = "计算两个数的积")
    public double multiply(double a, double b) {
        return a * b;
    }

    @Function(name = "除法", description = "计算两个数的商")
    public double divide(double a, double b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return a / b;
    }

    @Function(name = "平方根", description = "计算一个数的平方根")
    public double sqrt(double x) {
        if (x < 0) {
            throw new IllegalArgumentException("不能计算负数的平方根");
        }
        return Math.sqrt(x);
    }

    @Function(name = "幂运算", description = "计算一个数的幂")
    public double pow(double base, double exponent) {
        return Math.pow(base, exponent);
    }
}
