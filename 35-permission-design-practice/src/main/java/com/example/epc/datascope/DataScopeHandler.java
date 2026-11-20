package com.example.epc.datascope;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.example.epc.permission.PermissionCache;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限 SQL 注入器（MyBatis-Plus DataPermissionHandler 实现）。
 *
 * <p>只在编译期改写 SQL：给 com.example.epc.mapper.OrderMapper 的查询
 * 自动追加数据范围条件——</p>
 * <ul>
 *   <li>ALL：不加条件；</li>
 *   <li>SELF：{@code user_id = 当前用户}；</li>
 *   <li>其余：{@code dept_id IN (可见部门集合)}。</li>
 * </ul>
 *
 * <p>三个必须知道的边界（八股实证）：</p>
 * <ol>
 *   <li>只拦截 SELECT：UPDATE/DELETE 不走这条链路，所以详情/修改接口
 *   还要 {@link DataScopeValidator} 二次校验——单有 SQL 注入防不住
 *   「直接按 id 改别人数据」的水平越权；</li>
 *   <li>未登录请求（如登录接口本身）直接放行；</li>
 *   <li>表别名场景要对列名做别名适配，本章 SQL 没有 join 故从简，
 *   生产复杂查询建议配列名映射表。</li>
 * </ol>
 */
@Component
public class DataScopeHandler implements DataPermissionHandler {

    private final PermissionCache permissionCache;

    public DataScopeHandler(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 只作用于订单查询，用户/角色等管理接口不过滤
        if (!mappedStatementId.contains("OrderMapper")) {
            return where;
        }
        Long userId;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            return where; // 登录接口等白名单请求不过滤
        }

        DataScopeContext ctx = permissionCache.getDataScope(userId);
        Expression condition = buildCondition(ctx);
        if (condition == null) {
            return where;
        }
        return where == null ? condition : new AndExpression(where, condition);
    }

    private Expression buildCondition(DataScopeContext ctx) {
        switch (ctx.getType()) {
            case ALL:
                return null;
            case SELF:
                return new EqualsTo(new Column("user_id"), new LongValue(ctx.getUserId()));
            default:
                if (ctx.getDeptIds() == null || ctx.getDeptIds().isEmpty()) {
                    // 配置了部门范围但解析结果为空：用恒假条件挡住（默认拒绝）
                    return new EqualsTo(new Column("dept_id"), new LongValue(-1));
                }
                InExpression in = new InExpression();
                in.setLeftExpression(new Column("dept_id"));
                // jsqlparser 4.6 的 ExpressionList 实现 ItemsList（不是 Expression），
                // IN 右值要用 setRightItemsList；ExpressionList 自带括号渲染，无需再包 Parenthesis
                List<Expression> deptIdValues = ctx.getDeptIds().stream()
                        .map(id -> (Expression) new LongValue(id))
                        .collect(Collectors.toList());
                in.setRightItemsList(new ExpressionList(deptIdValues));
                return in;
        }
    }
}
