package com.example.vmp.validation.groups;

/**
 * 校验分组标记接口：区分「新增必填」与「编辑可空」。
 */
public class ValidationGroups {

    /** 新增场景 */
    public interface Create {
    }

    /** 编辑场景 */
    public interface Update {
    }
}
