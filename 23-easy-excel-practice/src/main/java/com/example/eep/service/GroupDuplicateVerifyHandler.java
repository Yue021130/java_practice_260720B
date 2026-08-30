package com.example.eep.service;

import cn.afterturn.easypoi.excel.entity.result.ExcelVerifyHandlerResult;
import cn.afterturn.easypoi.handler.inter.IExcelVerifyHandler;
import com.example.eep.excel.easypoi.SysUserImport;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 组内重复校验处理器。
 *
 * <p>使用 ThreadLocal 缓存本批次已校验数据，用于发现 Excel 内部重复行。</p>
 */
public class GroupDuplicateVerifyHandler implements IExcelVerifyHandler<SysUserImport> {

    private static final String PREFIX = "【";
    private static final String SUFFIX = "】";

    private final ThreadLocal<List<SysUserImport>> threadLocal = new ThreadLocal<>();

    @Override
    public ExcelVerifyHandlerResult verifyHandler(SysUserImport user) {
        StringJoiner joiner = new StringJoiner(", ", PREFIX, SUFFIX);

        if (!StringUtils.hasText(user.getRealname())) {
            joiner.add("用户姓名不能为空");
        }

        List<SysUserImport> exists = threadLocal.get();
        if (exists == null) {
            exists = new ArrayList<>();
        }

        // 检查本批次是否已存在同名用户
        for (SysUserImport e : exists) {
            if (StringUtils.hasText(e.getRealname()) && e.getRealname().equals(user.getRealname())) {
                joiner.add("与第 " + (e.getRowNum() + 1) + " 行重复");
                break;
            }
        }

        exists.add(user);
        threadLocal.set(exists);

        String result = joiner.toString();
        if (!"【】".equals(result)) {
            return new ExcelVerifyHandlerResult(false, result);
        }
        return new ExcelVerifyHandlerResult(true);
    }

    public ThreadLocal<List<SysUserImport>> getThreadLocal() {
        return threadLocal;
    }
}
