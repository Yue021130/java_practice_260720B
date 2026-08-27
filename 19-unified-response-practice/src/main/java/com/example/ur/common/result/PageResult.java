package com.example.ur.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果统一封装。
 *
 * <p>字段名从一开始就和前端约定死，避免每次联调都重新对字段。
 * 总页数在构造器里自动计算，防止除零。</p>
 *
 * @param <T> 列表元素类型
 */
@Data
@Schema(description = "统一分页结果封装：作为 Result.data 承载列表与分页元信息")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    @Schema(description = "当前页数据列表")
    private List<T> list;

    /** 总记录数 */
    @Schema(description = "总记录数", example = "10")
    private long total;

    /** 当前页码（从 1 开始） */
    @Schema(description = "当前页码，从 1 开始", example = "1")
    private long pageNum;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "3")
    private long pageSize;

    /** 总页数 */
    @Schema(description = "总页数（自动计算）", example = "4")
    private long pages;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long pageNum, long pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        // 防止除零，自动计算总页数
        this.pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
    }
}
