package com.example.ee.excel;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步导出任务状态。
 */
@Data
public class ExportTaskStatus {

    /** 任务 ID。 */
    private String taskId;

    /** 任务状态：PENDING / RUNNING / SUCCESS / FAILED。 */
    private String status;

    /** 已处理行数。 */
    private long processedRows;

    /** 总行数。 */
    private long totalRows;

    /** 文件下载路径。 */
    private String fileUrl;

    /** 错误信息。 */
    private String errorMsg;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 完成时间。 */
    private LocalDateTime finishTime;
}
