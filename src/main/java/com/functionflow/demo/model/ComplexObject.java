package com.functionflow.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 复杂对象示例
 * 用于演示内嵌对象作为函数参数和返回值
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexObject {
    
    /**
     * 基本信息
     */
    private String name;
    private String description;
    private Integer age;
    private Double score;
    
    /**
     * 内嵌对象
     */
    private Address address;
    private ContactInfo contactInfo;
    
    /**
     * 集合类型
     */
    private List<String> tags;
    private List<Address> addresses;
    private Map<String, Object> metadata;
    
    /**
     * 地址信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }
    
    /**
     * 联系信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {
        private String email;
        private String phone;
        private String website;
        private Address officeAddress;
    }
}
