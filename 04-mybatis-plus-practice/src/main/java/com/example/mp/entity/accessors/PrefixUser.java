package com.example.mp.entity.accessors;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Accessors(prefix = {"f", "m"}) 演示：字段前缀被自动剥离。
 *
 * 面试八股：
 * - prefix 用于处理类字段带前缀的遗留代码或规范，如 fName、mAge
 * - Lombok 生成 getter/setter 时会去掉前缀，外部调用 getName()/setAge()
 * - 常与 chain=true 一起使用
 */
@Data
@Accessors(chain = true, prefix = {"f", "m"})
public class PrefixUser {

    private String fName;
    private Integer mAge;
}
