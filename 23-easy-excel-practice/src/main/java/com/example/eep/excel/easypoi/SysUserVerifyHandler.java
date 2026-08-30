package com.example.eep.excel.easypoi;

import cn.afterturn.easypoi.excel.entity.result.ExcelVerifyHandlerResult;
import cn.afterturn.easypoi.handler.inter.IExcelVerifyHandler;
import com.example.eep.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 系统用户导入校验处理器。
 *
 * <p>实现 {@link IExcelVerifyHandler}，对每一行 Excel 数据进行业务级校验。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysUserVerifyHandler implements IExcelVerifyHandler<SysUserImport> {

    private static final String PREFIX = "【";
    private static final String SUFFIX = "】";

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final SysUserRepository sysUserRepository;

    @Override
    public ExcelVerifyHandlerResult verifyHandler(SysUserImport user) {
        StringJoiner joiner = new StringJoiner(", ", PREFIX, SUFFIX);

        if (!StringUtils.hasText(user.getRealname())) {
            joiner.add("用户姓名不能为空");
        }
        if (!StringUtils.hasText(user.getDeptOrgCode())) {
            joiner.add("部门编码不能为空");
        }
        if (!StringUtils.hasText(user.getRoleCode())) {
            joiner.add("角色编码不能为空");
        }

        if (StringUtils.hasText(user.getPhone()) && !MOBILE_PATTERN.matcher(user.getPhone()).matches()) {
            joiner.add("手机号格式不正确");
        }
        if (StringUtils.hasText(user.getEmail()) && !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            joiner.add("邮箱格式不正确");
        }

        // 模拟数据库唯一性校验：同名用户已存在
        if (StringUtils.hasText(user.getRealname()) && sysUserRepository.existsByRealname(user.getRealname())) {
            joiner.add("该姓名用户已存在");
        }

        String result = joiner.toString();
        if (!"【】".equals(result)) {
            return new ExcelVerifyHandlerResult(false, result);
        }
        return new ExcelVerifyHandlerResult(true);
    }
}
