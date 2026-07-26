package com.example.mp.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.incrementer.H2KeyGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.example.mp.extension.TableNameContext;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件与扫描配置。
 *
 * 面试点：
 * - 分页插件 PaginationInnerInterceptor 必须配置，否则 Page<T> 不生效。
 * - 乐观锁插件 OptimisticLockerInnerInterceptor 配合 @Version 实现并发控制。
 * - @MapperScan 替代每个 Mapper 上的 @Mapper，集中管理扫描路径。
 */
@Configuration
@MapperScan("com.example.mp.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：指定数据库类型为 H2
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防止全表更新删除插件（演示 @InterceptorIgnore 用）
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 动态表名插件（演示按月分表）
        DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
        dynamicTableNameInnerInterceptor.setTableNameHandler((sql, tableName) -> {
            if ("t_report".equals(tableName)) {
                String suffix = TableNameContext.get();
                return suffix == null ? tableName : tableName + "_" + suffix;
            }
            return tableName;
        });
        interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
        return interceptor;
    }

    /**
     * H2 序列主键生成器：供 @KeySequence 使用。
     */
    @Bean
    public H2KeyGenerator h2KeyGenerator() {
        return new H2KeyGenerator();
    }
}
