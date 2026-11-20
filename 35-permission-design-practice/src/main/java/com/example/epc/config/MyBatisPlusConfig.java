package com.example.epc.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.example.epc.datascope.DataScopeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页插件 + 数据权限拦截器。
 *
 * <p>拦截器顺序有讲究：DataPermissionInterceptor 必须在分页之前执行
 * （先注入 WHERE 条件再统计总数），MyBatis-Plus 内部按 add 顺序
 * 倒序执行 inner interceptor，这里保持「先加数据权限、后加分页」
 * 的声明顺序即可。</p>
 *
 * <p>DataPermissionInterceptor 的原理：在 Executor.query 阶段拿到
 * BoundSql 里的 WHERE 表达式（JSqlParser 解析的 AST），调用
 * {@link DataScopeHandler#getSqlSegment} 追加条件后重新生成 SQL——
 * 对 Mapper 接口零侵入，这是「数据权限方案对比」里拦截器方案
 * （对应参考文档 2 的生产级落地）的实现位置。</p>
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataScopeHandler dataScopeHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataScopeHandler));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
