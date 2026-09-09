package com.example.rbac.datapermission;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Component;

/**
 * 数据权限拦截处理器：
 * 对受控查询统一注入 WHERE 条件，代替散落在各 Mapper XML 中硬编码的
 * "WHERE dept_id = ?"（见 docs/《动态数据权限控制》——机制兜底，而不是人肉检查）。
 *
 * <p>规则：
 * <ul>
 *   <li>仅作用于 OrderMapper 的语句（订单是演示载体）</li>
 *   <li>GLOBAL（全部数据）：不加条件</li>
 *   <li>DEPT（本部门）：追加 dept_id = 当前用户部门</li>
 *   <li>SELF（本人）：追加 dept_id = 当前用户部门 AND user_id = 当前用户</li>
 * </ul>
 */
@Component
public class DataScopeHandler implements DataPermissionHandler {

    /** 受数据权限管控的 Mapper 命名空间关键字 */
    private static final String GUARDED_MAPPER = "OrderMapper";

    private final DataScopeContext dataScopeContext;

    public DataScopeHandler(DataScopeContext dataScopeContext) {
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 非订单查询不拦截
        if (!mappedStatementId.contains(GUARDED_MAPPER)) {
            return where;
        }
        DataScopeInfo scope = dataScopeContext.current();
        if (scope.isGlobal()) {
            return where;
        }
        Expression scoped = appendDeptCondition(where, scope.getDeptId());
        if (scope.isSelf()) {
            scoped = new AndExpression(scoped, eqColumn("user_id", scope.getUserId()));
        }
        return scoped;
    }

    /**
     * 追加 dept_id 条件（保持原有 where 不变）
     */
    private Expression appendDeptCondition(Expression where, long deptId) {
        Expression deptCondition = eqColumn("dept_id", deptId);
        return where == null ? deptCondition : new AndExpression(where, deptCondition);
    }

    private EqualsTo eqColumn(String column, long value) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(column));
        equalsTo.setRightExpression(new LongValue(value));
        return equalsTo;
    }
}
