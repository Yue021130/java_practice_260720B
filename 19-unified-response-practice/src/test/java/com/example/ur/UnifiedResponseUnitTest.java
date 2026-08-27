package com.example.ur;

import com.example.ur.common.result.BusinessException;
import com.example.ur.common.result.PageResult;
import com.example.ur.common.result.Result;
import com.example.ur.common.result.ResultCode;
import com.example.ur.common.result.ResultFactory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result / ResultFactory / PageResult / BusinessException 单元测试。
 */
public class UnifiedResponseUnitTest {

    @Test
    void successResultShouldHaveCorrectStructure() {
        Result<String> result = ResultFactory.success("hello");

        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getMsg()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("hello");
        assertThat(result.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void failedResultShouldUseEnumMessage() {
        Result<Void> result = ResultFactory.failed(ResultCode.NOT_FOUND);

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMsg()).isEqualTo("资源不存在");
        assertThat(result.getData()).isNull();
    }

    @Test
    void failedResultShouldSupportCustomMessage() {
        Result<Void> result = ResultFactory.failed("用户名已存在");

        assertThat(result.getCode()).isEqualTo(ResultCode.FAILED.getCode());
        assertThat(result.getMsg()).isEqualTo("用户名已存在");
    }

    @Test
    void pageResultShouldCalculatePages() {
        List<String> list = Arrays.asList("a", "b", "c");
        PageResult<String> page = new PageResult<>(list, 10, 1, 3);

        assertThat(page.getList()).hasSize(3);
        assertThat(page.getTotal()).isEqualTo(10);
        assertThat(page.getPageNum()).isEqualTo(1);
        assertThat(page.getPageSize()).isEqualTo(3);
        // 10 条 / 每页 3 条 = 4 页
        assertThat(page.getPages()).isEqualTo(4);
    }

    @Test
    void pageResultShouldAvoidDivideByZero() {
        PageResult<String> page = new PageResult<>(Arrays.asList(), 0, 1, 0);
        assertThat(page.getPages()).isEqualTo(0);
    }

    @Test
    void businessExceptionShouldCarryCode() {
        BusinessException ex = new BusinessException(ResultCode.UNAUTHORIZED);

        assertThat(ex.getCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo("暂未登录或登录已过期");
    }

    @Test
    void businessExceptionShouldSupportCustomCode() {
        BusinessException ex = new BusinessException(40001, "参数不合法");

        assertThat(ex.getCode()).isEqualTo(40001);
        assertThat(ex.getMessage()).isEqualTo("参数不合法");
    }
}
