package com.example.ur.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回结果封装。
 *
 * <p>设计思路：给"业务数据"和"传输语义"之间建立稳定契约。
 * 前端只需判断 body.code，无需关心每个接口的返回结构差异。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>code：业务状态码，0 表示成功，非 0 表示各种失败</li>
 *     <li>msg：提示信息，给用户看，异常时携带具体原因</li>
 *     <li>data：真正的业务数据，可为 null</li>
 *     <li>timestamp：服务端响应时间戳（毫秒），排查问题时很有用</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：0 成功，非 0 失败 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /** 服务端响应时间戳（毫秒） */
    private long timestamp;

    public Result(int code, String msg, T data) {
        this(code, msg, data, System.currentTimeMillis());
    }
}
