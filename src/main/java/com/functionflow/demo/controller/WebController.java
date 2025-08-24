package com.functionflow.demo.controller;

import com.functionflow.demo.core.FunctionScanner;
import com.functionflow.demo.model.FunctionSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Web界面控制器
 */
@Controller
public class WebController {
    
    @Autowired
    private FunctionScanner discoveryService;
    
    /**
     * 主页
     */
    @GetMapping("/")
    public String index(Model model) {
        List<FunctionSchema> functions = discoveryService.getAllFunctions();
        model.addAttribute("functions", functions);
        return "index";
    }
    
    /**
     * 函数列表页面
     */
    @GetMapping("/functions")
    public String functions(Model model) {
        List<FunctionSchema> functions = discoveryService.getAllFunctions();
        model.addAttribute("functions", functions);
        return "functions";
    }
    

}
