package com.example.sl.parallel;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ParallelService {

    public Map<String, Object> speedupDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Integer> numbers = IntStream.rangeClosed(1, 500_000)
                .boxed()
                .collect(Collectors.toList());

        long serialStart = System.currentTimeMillis();
        long serialSum = numbers.stream()
                .mapToLong(this::cpuHeavyWork)
                .sum();
        long serialMs = System.currentTimeMillis() - serialStart;

        long parallelStart = System.currentTimeMillis();
        long parallelSum = numbers.parallelStream()
                .mapToLong(this::cpuHeavyWork)
                .sum();
        long parallelMs = System.currentTimeMillis() - parallelStart;

        result.put("elementCount", numbers.size());
        result.put("serialMs", serialMs);
        result.put("parallelMs", parallelMs);
        result.put("speedup", String.format("%.2fx", (double) serialMs / Math.max(parallelMs, 1)));
        result.put("serialSum", serialSum);
        result.put("parallelSum", parallelSum);
        result.put("interviewNote", "CPU 密集型大集合适合 parallelStream，利用 ForkJoinPool.commonPool 多核并行；结果应相等。");
        return result;
    }

    private long cpuHeavyWork(int n) {
        BigInteger value = BigInteger.valueOf(n);
        return value.isProbablePrime(10) ? value.longValue() : (long) (Math.pow(n % 100 + 1, 1.1) * Math.sin(n));
    }

    public Map<String, Object> overheadDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Integer> smallList = IntStream.rangeClosed(1, 100)
                .boxed()
                .collect(Collectors.toList());

        int loops = 10_000;

        long serialStart = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            smallList.stream().map(n -> n + 1).collect(Collectors.toList());
        }
        long serialNs = System.nanoTime() - serialStart;

        long parallelStart = System.nanoTime();
        for (int i = 0; i < loops; i++) {
            smallList.parallelStream().map(n -> n + 1).collect(Collectors.toList());
        }
        long parallelNs = System.nanoTime() - parallelStart;

        result.put("elementCount", smallList.size());
        result.put("loops", loops);
        result.put("serialNs", serialNs);
        result.put("parallelNs", parallelNs);
        result.put("overheadFactor", String.format("%.2fx", (double) parallelNs / Math.max(serialNs, 1)));
        result.put("interviewNote", "小集合或简单操作使用 parallelStream 会因任务拆分/线程调度开销反而更慢。");
        return result;
    }

    public Map<String, Object> raceConditionDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Integer> source = IntStream.rangeClosed(1, 100_000)
                .boxed()
                .collect(Collectors.toList());

        List<Integer> shared = new ArrayList<>();

        source.parallelStream()
                .forEach(shared::add);

        int expected = source.size();
        int actual = shared.size();

        result.put("expected", expected);
        result.put("actual", actual);
        result.put("matched", expected == actual);
        result.put("interviewNote", "parallelStream().forEach(list::add) 把元素写入共享可变集合会产生竞争，实际 size 通常小于预期。");
        return result;
    }

    public Map<String, Object> correctReduceDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Integer> numbers = IntStream.rangeClosed(1, 1_000_000)
                .boxed()
                .collect(Collectors.toList());

        long reduceSum = numbers.parallelStream()
                .mapToLong(Integer::longValue)
                .reduce(0L, Long::sum);

        long collectSum = numbers.parallelStream()
                .collect(Collectors.summingLong(Integer::longValue));

        long expected = numbers.stream()
                .mapToLong(Integer::longValue)
                .sum();

        result.put("expected", expected);
        result.put("reduceSum", reduceSum);
        result.put("collectSum", collectSum);
        result.put("interviewNote", "并行聚合应使用无状态、满足结合律的 reduce 或 collect（如 summingInt），结果始终正确。");
        return result;
    }

    public Map<String, Object> orderFindAnyDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Integer> ordered = IntStream.rangeClosed(1, 1_000_000)
                .boxed()
                .collect(Collectors.toList());

        int findFirstOrdered = ordered.parallelStream()
                .filter(n -> n > 500_000)
                .findFirst()
                .orElse(-1);

        int findAnyOrdered = ordered.parallelStream()
                .filter(n -> n > 500_000)
                .findAny()
                .orElse(-1);

        int findFirstUnordered = ordered.parallelStream()
                .unordered()
                .filter(n -> n > 500_000)
                .findFirst()
                .orElse(-1);

        int findAnyUnordered = ordered.parallelStream()
                .unordered()
                .filter(n -> n > 500_000)
                .findAny()
                .orElse(-1);

        result.put("findFirstOrdered", findFirstOrdered);
        result.put("findAnyOrdered", findAnyOrdered);
        result.put("findFirstUnordered", findFirstUnordered);
        result.put("findAnyUnordered", findAnyUnordered);
        result.put("interviewNote", "有序流 parallelStream 中 findFirst 稳定返回首个，findAny 可返回任意匹配元素；调用 unordered() 可取消顺序约束提升并行效率。");
        return result;
    }
}
