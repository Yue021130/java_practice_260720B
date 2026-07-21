package com.example.juc.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 场景运行结果：所有 JUC 实验场景的统一返回结构。
 *
 * 前端拿到后渲染两部分：
 * - steps：带时间戳 + 线程名的「时间线日志」，直观看到多线程交错执行；
 * - data：关键指标（如最终结果、耗时对比、峰值并发数），渲染成指标卡。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "场景运行结果")
public class ScenarioResult {

    /** 一句话结论（这个场景演示了什么、结果说明了什么） */
    @Schema(description = "一句话结论")
    private String summary;

    /** 场景总耗时（毫秒） */
    @Schema(description = "场景总耗时（毫秒）", example = "1234")
    private long elapsedMs;

    /** 时间线日志，按发生顺序排列 */
    @Schema(description = "时间线日志，按发生顺序排列")
    private List<Step> steps;

    /** 关键指标，键值对形式，前端渲染成指标卡 */
    @Schema(description = "关键指标")
    private Map<String, Object> data;

    /**
     * 从日志收集器构建结果。
     *
     * @param log     场景运行期间使用的日志收集器
     * @param summary 一句话结论
     * @param data    关键指标（可为 null）
     */
    public static ScenarioResult of(ScenarioLog log, String summary, Map<String, Object> data) {
        return new ScenarioResult(summary, log.elapsedMs(), log.getSteps(), data);
    }

    /**
     * 时间线日志的一条记录。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "时间线日志的一条记录")
    public static class Step {

        /** 距场景开始的毫秒偏移 */
        @Schema(description = "距场景开始的毫秒偏移", example = "42")
        private long t;

        /** 产生该日志的线程名（并发可视化的核心：看线程交错） */
        @Schema(description = "产生该日志的线程名", example = "transfer-t1")
        private String thread;

        /** 日志内容 */
        @Schema(description = "日志内容")
        private String message;
    }
}
