package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.annotation.Function;
import org.springframework.stereotype.Component;

/**
 * 字符串处理函数示例
 */
@Component
@Functions(name = "字符串函数", description = "字符串处理函数集合", category = "字符串处理", version = "1.0.0")
public class StringFunctions implements FunctionService {
    
    @Function(name = "字符串连接", description = "连接两个字符串")
    public String concat(String str1, String str2) {
        return str1 + str2;
    }
    
    @Function(name = "字符串转大写", description = "将字符串转换为大写")
    public String toUpperCase(String text) {
        return text.toUpperCase();
    }
    
    @Function(name = "字符串转小写", description = "将字符串转换为小写")
    public String toLowerCase(String text) {
        return text.toLowerCase();
    }
    
    @Function(name = "字符串长度", description = "计算字符串长度")
    public int length(String text) {
        return text.length();
    }
    
    @Function(name = "字符串截取", description = "截取字符串的一部分")
    public String substring(String text, int start, Integer end) {
        if (end == null) {
            return text.substring(start);
        } else {
            return text.substring(start, end);
        }
    }
    
    @Function(name = "字符串替换", description = "替换字符串中的内容")
    public String replace(String text, String oldStr, String newStr) {
        return text.replace(oldStr, newStr);
    }
}
