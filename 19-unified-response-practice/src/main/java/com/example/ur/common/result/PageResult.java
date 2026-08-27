package com.example.ur.common.result;

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
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /** 总页数 */
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
