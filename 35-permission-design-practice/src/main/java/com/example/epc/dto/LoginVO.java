package com.example.epc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录成功出参：JWT token + 用户基本信息 + 可见菜单树 + 按钮权限点集合。
 *
 * <p>menus/perms 一次下发，前端据此渲染菜单并控制按钮显隐，
 * 后端接口仍逐请求用 @SaCheckPermission 强校验（前端隐藏只是体验，不是安全）。</p>
 */
@Data
public class LoginVO implements Serializable {

    /** JWT Token，前端存入 Pinia 并随每次请求放入 Authorization 请求头 */
    private String token;

    private Long id;

    private String username;

    private String nickname;

    /** 该用户可见的 MENU 类型权限树 */
    private List<MenuVO> menus;

    /** 该用户拥有的 BUTTON 类型权限点标识集合 */
    private List<String> perms;
}
