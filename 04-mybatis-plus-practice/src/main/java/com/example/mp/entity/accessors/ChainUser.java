package com.example.mp.entity.accessors;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Accessors(chain = true) 演示：setter 返回 this，可链式调用。
 *
 * 面试八股：
 * - chain=true 让 setXxx() 返回对象本身，支持 user.setName("a").setAge(18)
 * - 不改变 getter/setter 方法名，JSON 序列化不受影响
 * - 常与 @Data 或 @Setter 配合使用
 */
@Data
@Accessors(chain = true)
public class ChainUser {

    private Long id;
    private String name;
    private Integer age;
}
