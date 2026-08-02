package com.example.async.task;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 批量异步任务 + 结果聚合：构造多个 CompletableFuture，用 allOf 汇总。
 */
@Service
@RequiredArgsConstructor
public class BatchTaskService {

    private final AsyncTaskService asyncTaskService;

    /**
     * 提交 5 个 IO 型异步任务，汇总结果与耗时。
     */
    public BatchResult runBatchAggregate() {
        long start = System.currentTimeMillis();

        List<CompletableFuture<Integer>> futures = IntStream.rangeClosed(1, 5)
                .mapToObj(asyncTaskService::mockIoTask)
                .collect(Collectors.toList());

        CompletableFuture<List<Integer>> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        List<Integer> values = all.join();
        int sum = values.stream().mapToInt(Integer::intValue).sum();
        long cost = System.currentTimeMillis() - start;

        return new BatchResult(values, sum, cost);
    }

    /**
     * 批量任务返回值包装（内部 DTO）。
     */
    public static class BatchResult {
        private final List<Integer> values;
        private final int sum;
        private final long costMs;

        public BatchResult(List<Integer> values, int sum, long costMs) {
            this.values = values;
            this.sum = sum;
            this.costMs = costMs;
        }

        public List<Integer> getValues() {
            return values;
        }

        public int getSum() {
            return sum;
        }

        public long getCostMs() {
            return costMs;
        }
    }
}
