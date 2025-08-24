package com.functionflow.demo.functions;
import com.functionflow.demo.annotation.Input;
import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.annotation.Output;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
/**
 * 字符串处理函数示例
 */
@Component
@Functions(name = "字符串函数", description = "字符串处理函数集合", category = "字符串处理", version = "1.0.0")
public class StringFunctions {
    @Function(
        name = "字符串连接",
        description = "连接两个字符串" 
    )
    public @Output(name = "result", description = "连接后的字符串", type = String.class) String concat(
            @Input(name = "str1", description = "第一个字符串", type = String.class, required = true) 
            @NotNull @Size(min = 1) String str1,
            @Input(name = "str2", description = "第二个字符串", type = String.class, required = true) 
            @NotNull @Size(min = 1) String str2) {
        return str1 + str2;
    }
    @Function(
        name = "字符串转大写",
        description = "将字符串转换为大写"
    )
    public @Output(name = "result", description = "大写字符串", type = String.class) String toUpperCase(
            @Input(name = "text", description = "要转换的字符串", type = String.class, required = true) 
            @NotNull String text) {
        return text.toUpperCase();
    }
    @Function(
        name = "字符串转小写",
        description = "将字符串转换为小写"
    )
    public @Output(name = "result", description = "小写字符串", type = String.class) String toLowerCase(
            @Input(name = "text", description = "要转换的字符串", type = String.class, required = true) 
            @NotNull String text) {
        return text.toLowerCase();
    }
    @Function(
        name = "字符串长度",
        description = "计算字符串长度"
    )
    public @Output(name = "length", description = "字符串长度", type = Integer.class) int length(
            @Input(name = "text", description = "要计算长度的字符串", type = String.class, required = true) 
            @NotNull String text) {
        return text.length();
    }
    @Function(
        name = "字符串截取",
        description = "截取字符串的一部分"
    )
    public @Output(name = "substring", description = "截取的字符串", type = String.class) String substring(
            @Input(name = "text", description = "原字符串", type = String.class, required = true) 
            @NotNull String text,
            @Input(name = "start", description = "开始位置", type = Integer.class, required = true) 
            int start,
            @Input(name = "end", description = "结束位置", type = Integer.class, required = false) 
            Integer end) {
        if (end == null) {
            return text.substring(start);
        } else {
            return text.substring(start, end);
        }
    }
    @Function(
        name = "字符串替换",
        description = "替换字符串中的内容"
    )
    @Output(name = "result", description = "替换后的字符串", type = String.class) 
    public String replace(
            @Input(name = "text", description = "原字符串", type = String.class, required = true) 
            @NotNull String text,
            @Input(name = "oldStr", description = "要替换的字符串", type = String.class, required = true) 
            @NotNull String oldStr,
            @Input(name = "newStr", description = "新的字符串", type = String.class, required = true) 
            @NotNull String newStr) {
        return text.replace(oldStr, newStr);
    }
}
