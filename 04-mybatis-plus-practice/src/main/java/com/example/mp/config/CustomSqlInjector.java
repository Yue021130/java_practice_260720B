package com.example.mp.config;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义 SQL 注入器：扩展 BaseMapper 的通用方法。
 *
 * 面试八股：
 * - DefaultSqlInjector 是 MyBatis-Plus 默认的 SQL 注入器
 * - 通过继承它并添加 AbstractMethod，可以让所有 Mapper 都拥有额外方法
 * - InsertBatchSomeColumn 是 MP 提供的批量插入实现，一条 SQL 插入多行，性能远高于 for 循环单条 insert
 */
@Component
public class CustomSqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass, tableInfo);
        // 为所有 Mapper 注入 insertBatchSomeColumn 方法
        methodList.add(new InsertBatchSomeColumn());
        return methodList;
    }
}
