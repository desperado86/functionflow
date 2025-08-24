package com.functionflow.demo.functions;

import com.functionflow.demo.annotation.Functions;
import com.functionflow.demo.annotation.Function;
import com.functionflow.demo.model.ComplexObject;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复杂对象函数示例
 * 演示内嵌对象作为函数参数和返回值
 */
@Component
@Functions(name = "复杂对象函数", description = "处理复杂对象的函数集合", category = "对象处理", version = "1.0.0")
public class ComplexObjectFunctions {
    
    /**
     * 创建复杂对象
     */
    @Function(name = "创建复杂对象", description = "根据基本信息创建复杂对象")
    public ComplexObject createComplexObject(
            @NotNull(message = "name is required") 
            @Size(min = 2, max = 50, message = "name length must be between 2 and 50")
            String name, 
            
            @NotNull(message = "age is required")
            @Min(value = 0, message = "age must be positive")
            @Max(value = 150, message = "age must be less than 150")
            Integer age, 
            
            @NotNull(message = "email is required")
            @Email(message = "email format is invalid")
            @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "email pattern is invalid")
            String email) {
        
        ComplexObject.Address address = ComplexObject.Address.builder()
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .build();
        
        ComplexObject.ContactInfo contactInfo = ComplexObject.ContactInfo.builder()
                .email(email)
                .phone("+1-555-0123")
                .website("https://example.com")
                .officeAddress(address)
                .build();
        
        List<String> tags = new ArrayList<>();
        tags.add("user");
        tags.add("active");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createdBy", "system");
        metadata.put("version", "1.0");
        
        return ComplexObject.builder()
                .name(name)
                .description("Complex object for " + name)
                .age(age)
                .score(85.5)
                .address(address)
                .contactInfo(contactInfo)
                .tags(tags)
                .metadata(metadata)
                .build();
    }
    
    /**
     * 处理复杂对象
     */
    @Function(name = "处理复杂对象", description = "处理复杂对象并返回修改后的对象")
    public ComplexObject processComplexObject(ComplexObject object) {
        
        // 修改对象属性
        object.setName(object.getName() + " (Processed)");
        object.setScore(object.getScore() + 10.0);
        
        // 添加新标签
        if (object.getTags() == null) {
            object.setTags(new ArrayList<>());
        }
        object.getTags().add("processed");
        
        // 更新元数据
        if (object.getMetadata() == null) {
            object.setMetadata(new HashMap<>());
        }
        object.getMetadata().put("processedAt", System.currentTimeMillis());
        object.getMetadata().put("processor", "ComplexObjectFunctions");
        
        return object;
    }
    
    /**
     * 提取对象信息
     */
    @Function(name = "提取对象信息", description = "从复杂对象中提取基本信息")
    public Map<String, Object> extractObjectInfo(ComplexObject object) {
        
        Map<String, Object> info = new HashMap<>();
        info.put("name", object.getName());
        info.put("age", object.getAge());
        info.put("score", object.getScore());
        info.put("email", object.getContactInfo() != null ? object.getContactInfo().getEmail() : null);
        info.put("city", object.getAddress() != null ? object.getAddress().getCity() : null);
        info.put("tagCount", object.getTags() != null ? object.getTags().size() : 0);
        info.put("hasMetadata", object.getMetadata() != null && !object.getMetadata().isEmpty());
        
        return info;
    }
    
    /**
     * 验证复杂对象
     */
    @Function(name = "验证复杂对象", description = "验证复杂对象的完整性")
    public Map<String, Object> validateComplexObject(ComplexObject object) {
        
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 验证基本信息
        if (object.getName() == null || object.getName().trim().isEmpty()) {
            errors.add("Name is required");
        }
        
        if (object.getAge() == null || object.getAge() < 0) {
            errors.add("Age must be a positive number");
        }
        
        if (object.getAge() != null && object.getAge() > 150) {
            warnings.add("Age seems unusually high");
        }
        
        // 验证地址信息
        if (object.getAddress() != null) {
            if (object.getAddress().getCity() == null || object.getAddress().getCity().trim().isEmpty()) {
                warnings.add("City is missing in address");
            }
        }
        
        // 验证联系信息
        if (object.getContactInfo() != null) {
            if (object.getContactInfo().getEmail() == null || object.getContactInfo().getEmail().trim().isEmpty()) {
                warnings.add("Email is missing in contact info");
            }
        }
        
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("errorCount", errors.size());
        result.put("warningCount", warnings.size());
        
        return result;
    }
    
    /**
     * 合并复杂对象
     */
    @Function(name = "合并复杂对象", description = "合并两个复杂对象")
    public ComplexObject mergeComplexObjects(ComplexObject object1, ComplexObject object2) {
        
        // 合并基本信息
        String mergedName = object1.getName() + " + " + object2.getName();
        Integer mergedAge = (object1.getAge() + object2.getAge()) / 2;
        Double mergedScore = (object1.getScore() + object2.getScore()) / 2;
        
        // 合并标签
        List<String> mergedTags = new ArrayList<>();
        if (object1.getTags() != null) {
            mergedTags.addAll(object1.getTags());
        }
        if (object2.getTags() != null) {
            mergedTags.addAll(object2.getTags());
        }
        
        // 合并元数据
        Map<String, Object> mergedMetadata = new HashMap<>();
        if (object1.getMetadata() != null) {
            mergedMetadata.putAll(object1.getMetadata());
        }
        if (object2.getMetadata() != null) {
            mergedMetadata.putAll(object2.getMetadata());
        }
        mergedMetadata.put("mergedAt", System.currentTimeMillis());
        
        return ComplexObject.builder()
                .name(mergedName)
                .description("Merged object from " + object1.getName() + " and " + object2.getName())
                .age(mergedAge)
                .score(mergedScore)
                .address(object1.getAddress()) // 使用第一个对象的地址
                .contactInfo(object1.getContactInfo()) // 使用第一个对象的联系信息
                .tags(mergedTags)
                .metadata(mergedMetadata)
                .build();
    }
}
