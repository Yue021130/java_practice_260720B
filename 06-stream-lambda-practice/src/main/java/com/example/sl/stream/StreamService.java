package com.example.sl.stream;

import com.example.sl.data.MockDataGenerator;
import com.example.sl.domain.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StreamService {

    private final MockDataGenerator mockData;

    public Map<String, Object> createDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Integer> collectionStream = mockData.getEmployees().stream()
                .limit(3)
                .map(e -> e.getSalary())
                .collect(Collectors.toList());

        List<String> ofStream = Stream.of("a", "b", "c").collect(Collectors.toList());
        int rangeSum = IntStream.range(1, 6).sum();
        int rangeClosedSum = IntStream.rangeClosed(1, 5).sum();
        List<Integer> iterateList = Stream.iterate(1, n -> n * 2)
                .limit(5)
                .collect(Collectors.toList());
        List<Double> generateList = Stream.generate(Math::random)
                .limit(3)
                .collect(Collectors.toList());

        result.put("collectionStream", collectionStream);
        result.put("ofStream", ofStream);
        result.put("rangeSum", rangeSum);
        result.put("rangeClosedSum", rangeClosedSum);
        result.put("iterateList", iterateList);
        result.put("generateList", generateList);
        result.put("interviewNote", "Stream 不会修改数据源，可通过集合、数组、Stream.of、IntStream.range/rangeClosed、iterate、generate 创建。");
        return result;
    }

    public Map<String, Object> intermediateDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Employee> employees = mockData.getEmployees();

        List<String> rAndDHighSalary = employees.stream()
                .filter(e -> "研发".equals(e.getDept()))
                .filter(e -> e.getSalary() > 20000)
                .map(Employee::getName)
                .limit(5)
                .collect(Collectors.toList());

        List<Integer> distinctAges = employees.stream()
                .map(Employee::getAge)
                .distinct()
                .sorted()
                .limit(10)
                .collect(Collectors.toList());

        List<String> flatDepts = Stream.of(Arrays.asList("研发", "测试"), Arrays.asList("产品", "运维"))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<String> peeked = employees.stream()
                .filter(e -> e.getSalary() > 30000)
                .peek(e -> System.out.println("peek: " + e.getName()))
                .map(Employee::getName)
                .limit(3)
                .collect(Collectors.toList());

        List<Employee> sortedBySalary = employees.stream()
                .sorted(Comparator.comparingInt(Employee::getSalary).reversed())
                .limit(3)
                .collect(Collectors.toList());

        result.put("rAndDHighSalary", rAndDHighSalary);
        result.put("distinctAges", distinctAges);
        result.put("flatDepts", flatDepts);
        result.put("peekedHighSalary", peeked);
        result.put("top3BySalary", sortedBySalary.stream().map(e -> e.getName() + ":" + e.getSalary()).collect(Collectors.toList()));
        result.put("interviewNote", "中间操作返回 Stream 且懒执行：filter/map/flatMap/distinct/sorted/peek/limit/skip；peek 仅用于调试，不应修改元素。");
        return result;
    }

    public Map<String, Object> terminalDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Employee> employees = mockData.getEmployees();

        long rAndDCount = employees.stream()
                .filter(e -> "研发".equals(e.getDept()))
                .count();

        Optional<Employee> maxSalary = employees.stream()
                .max(Comparator.comparingInt(Employee::getSalary));

        boolean anyHighSalary = employees.stream()
                .anyMatch(e -> e.getSalary() > 40000);

        Optional<Employee> firstRAndD = employees.stream()
                .filter(e -> "研发".equals(e.getDept()))
                .findFirst();

        int totalSalary = employees.stream()
                .mapToInt(Employee::getSalary)
                .sum();

        List<String> names = employees.stream()
                .limit(5)
                .map(Employee::getName)
                .collect(Collectors.toList());

        result.put("rAndDCount", rAndDCount);
        result.put("maxSalaryEmployee", maxSalary.map(e -> e.getName() + ":" + e.getSalary()).orElse("N/A"));
        result.put("anyHighSalary", anyHighSalary);
        result.put("firstRAndD", firstRAndD.map(Employee::getName).orElse("N/A"));
        result.put("totalSalary", totalSalary);
        result.put("first5Names", names);
        result.put("interviewNote", "终止操作触发实际计算：collect/reduce/forEach/findFirst/anyMatch/max/min/count；Stream 只能消费一次。");
        return result;
    }

    public Map<String, Object> primitiveDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Employee> employees = mockData.getEmployees();

        int sumSalary = employees.stream()
                .mapToInt(Employee::getSalary)
                .sum();

        double avgAge = employees.stream()
                .mapToInt(Employee::getAge)
                .average()
                .orElse(0.0);

        long boxingSum = employees.stream()
                .map(Employee::getSalary)
                .reduce(0, Integer::sum);

        long primitiveStart = System.nanoTime();
        int primitiveSum = employees.stream()
                .mapToInt(Employee::getSalary)
                .sum();
        long primitiveNs = System.nanoTime() - primitiveStart;

        long boxedStart = System.nanoTime();
        int boxedSum = employees.stream()
                .map(Employee::getSalary)
                .reduce(0, Integer::sum);
        long boxedNs = System.nanoTime() - boxedStart;

        List<Integer> boxedRange = IntStream.rangeClosed(1, 5)
                .boxed()
                .collect(Collectors.toList());

        result.put("sumSalary", sumSalary);
        result.put("avgAge", String.format("%.2f", avgAge));
        result.put("boxingSum", boxingSum);
        result.put("primitiveSum", primitiveSum);
        result.put("boxedSum", boxedSum);
        result.put("primitiveNs", primitiveNs);
        result.put("boxedNs", boxedNs);
        result.put("boxedRange", boxedRange);
        result.put("interviewNote", "IntStream/LongStream/DoubleStream 避免装箱拆箱，提供 sum/average/max/range 等专用聚合；mapToInt 后可用 boxed() 转回 Stream<Integer>。");
        return result;
    }
}
