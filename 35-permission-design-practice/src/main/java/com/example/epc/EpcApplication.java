package com.example.epc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第35章启动类：企业级权限中心实战。
 *
 * <p>业务背景：后台管理系统的权限体系——RBAC 五表 + 部门树 +
 * 规则表驱动的五级数据范围 + 权限缓存与主动失效 + 水平越权二次校验
 * + @OpLog 操作审计 + 动态路由下发 + 按钮级权限指令。</p>
 *
 * <p>授权是默认拒绝的：除 application.yml / AuthController 中明确
 * 放行的白名单（/api/auth/login、/doc.html 等）外，所有接口都需要
 * 登录，业务接口再按 @SaCheckPermission 校验权限点（对应参考文档
 * 番外篇「权限可配置化之后的授权设计」）。</p>
 */
@SpringBootApplication
@MapperScan("com.example.epc.mapper")
public class EpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(EpcApplication.class, args);
        System.out.println("\n>>> 第35章启动成功，Knife4j 文档: http://localhost:8080/doc.html <<<\n");
    }
}
