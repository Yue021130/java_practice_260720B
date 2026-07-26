package com.example.mp.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户性别枚举：演示 @EnumValue 与 @JsonValue。
 *
 * 面试点：
 * - @EnumValue 标记哪个字段持久化到数据库。
 * - 通常配合 @JsonValue 控制返回给前端的展示值。
 * - 不使用 @EnumValue 时，MP 默认按枚举名 name() 持久化。
 */
@Getter
public enum UserGender {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    @EnumValue
    private final int code;

    @JsonValue
    private final String desc;

    UserGender(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
