package com.example.sl.collectors;

import com.example.sl.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/collectors")
@RequiredArgsConstructor
@Tag(name = "Collectors", description = "分组、分区、拼接与统计")
public class CollectorsController {

    private final CollectorsService collectorsService;

    @PostMapping("/group-partition")
    @Operation(summary = "分组与分区", description = "groupingBy / partitioningBy / counting / averagingInt")
    public ApiResponse<Map<String, Object>> groupPartition() {
        return ApiResponse.success(collectorsService.groupPartitionDemo());
    }

    @PostMapping("/join-summary")
    @Operation(summary = "字符串拼接与统计", description = "joining / summarizingInt / maxBy / reducing")
    public ApiResponse<Map<String, Object>> joinSummary() {
        return ApiResponse.success(collectorsService.joinSummaryDemo());
    }
}
