package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.Input;
import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Output;

import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotNull;

/**
 * 数学函数示例
 */
@Component
@Functions(name = "数学函数", description = "基础数学运算函数集合", category = "数学运算", version = "1.0.0")
public class MathFunctions {

    @Function(name = "加法", description = "计算两个数的和")
                  public @Output(name = "sum", description = "两数之和", type = Double.class) double add(
                      @Input(name = "a", description = "第一个数", type = Double.class, required = true) double a,
                      @Input(name = "b", description = "第二个数", type = Double.class, required = true) double b) {

        return a + b;
    }

    @Function(name = "减法", description = "计算两个数的差")

    public @Output(name = "difference", description = "两数之差", type = Double.class) double subtract(
            @Input(name = "a", description = "被减数", type = Double.class, required = true) double a,
            @Input(name = "b", description = "减数", type = Double.class, required = true) double b) {
        return a - b;
    }

    @Function(name = "乘法", description = "计算两个数的积")
    public @Output(name = "product", description = "两数之积", type = Double.class) double multiply(
            @Input(name = "a", description = "第一个数", type = Double.class, required = true) double a,
            @Input(name = "b", description = "第二个数", type = Double.class, required = true) double b) {
        return a * b;
    }

    @Function(name = "除法", description = "计算两个数的商")
    public @Output(name = "quotient", description = "两数之商", type = Double.class) double divide(
            @Input(name = "a", description = "被除数", type = Double.class, required = true) double a,
            @Input(name = "b", description = "除数", type = Double.class, required = true) double b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return a / b;
    }

    @Function(name = "平方根", description = "计算一个数的平方根")
    public @Output(name = "sqrt", description = "平方根", type = Double.class) double sqrt(
            @Input(name = "x", description = "要计算平方根的数", type = Double.class, required = true) double x) {
        if (x < 0) {
            throw new IllegalArgumentException("不能计算负数的平方根");
        }
        return Math.sqrt(x);
    }

    @Function(name = "幂运算", description = "计算一个数的幂")
    public @Output(name = "power", description = "幂运算结果", type = Double.class) double pow(
            @Input(name = "base", description = "底数", type = Double.class, required = true) double base,
            @Input(name = "exponent", description = "指数", type = Double.class, required = true) double exponent) {
        return Math.pow(base, exponent);
    }
}
