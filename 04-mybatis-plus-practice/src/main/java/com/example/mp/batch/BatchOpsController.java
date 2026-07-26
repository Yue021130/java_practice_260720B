package com.example.mp.batch;

import com.example.mp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 批量操作演示。
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Tag(name = "批量操作", description = "saveBatch / updateBatchById")
public class BatchOpsController {

    private final BatchOpsService batchOpsService;

    @PostMapping("/save-batch")
    @Operation(summary = "批量插入", description = "saveBatch 分批插入多条记录")
    public ApiResponse<Map<String, Object>> saveBatch() {
        return ApiResponse.success(batchOpsService.saveBatchDemo());
    }

    @PostMapping("/update-batch")
    @Operation(summary = "批量更新", description = "updateBatchById 按主键批量更新")
    public ApiResponse<Map<String, Object>> updateBatch() {
        return ApiResponse.success(batchOpsService.updateBatchDemo());
    }
}
