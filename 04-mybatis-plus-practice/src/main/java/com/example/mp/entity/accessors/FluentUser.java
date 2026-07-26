package com.example.mp.entity.accessors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Accessors(fluent = true) 演示：getter/setter 省略 get/set 前缀。
 *
 * 面试八股：
 * - fluent=true 会生成 name() / name(String) 风格的方法，同时隐含 chain=true
 * - 代码更简洁，但会破坏 JavaBean 规范，部分框架（如 Jackson）默认识别不了
 * - 需要配合 @JsonAutoDetect 或手动加 @JsonProperty 才能正常序列化
 */
@Data
@Accessors(fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FluentUser {

    private Long id;
    private String name;
}
