package com.example.sl.collectors;

import com.example.sl.data.MockDataGenerator;
import com.example.sl.domain.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectorsService {

    private final MockDataGenerator mockData;

    public Map<String, Object> groupPartitionDemo() {
        Map<String, Object> result = new java.util.HashMap<>();

        List<Employee> employees = mockData.getEmployees();

        Map<String, Long> deptCount = employees.stream()
                .collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));

        Map<String, Double> deptAvgSalary = employees.stream()
                .collect(Collectors.groupingBy(Employee::getDept, Collectors.averagingInt(Employee::getSalary)));

        Map<Boolean, List<Employee>> highSalaryPartition = employees.stream()
                .collect(Collectors.partitioningBy(e -> e.getSalary() > 10000));

        Map<Boolean, Long> highSalaryCount = employees.stream()
                .collect(Collectors.partitioningBy(e -> e.getSalary() > 10000, Collectors.counting()));

        result.put("deptCount", deptCount);
        result.put("deptAvgSalary", deptAvgSalary);
        Map<Boolean, Integer> highSalaryPartitionSize = new java.util.HashMap<>();
        highSalaryPartitionSize.put(true, highSalaryPartition.get(true).size());
        highSalaryPartitionSize.put(false, highSalaryPartition.get(false).size());
        result.put("highSalaryPartitionSize", highSalaryPartitionSize);
        result.put("highSalaryCount", highSalaryCount);
        result.put("interviewNote", "groupingBy 按属性分组，partitioningBy 按 boolean 二分；可嵌套 counting/averagingInt/summingInt 等下游收集器。");
        return result;
    }

    public Map<String, Object> joinSummaryDemo() {
        Map<String, Object> result = new java.util.HashMap<>();

        List<Employee> employees = mockData.getEmployees();

        String joinedNames = employees.stream()
                .limit(5)
                .map(Employee::getName)
                .collect(Collectors.joining(", "));

        IntSummaryStatistics ageStats = employees.stream()
                .collect(Collectors.summarizingInt(Employee::getAge));

        Optional<Employee> maxSalary = employees.stream()
                .collect(Collectors.maxBy(Comparator.comparingInt(Employee::getSalary)));

        Integer totalSalary = employees.stream()
                .collect(Collectors.reducing(0, Employee::getSalary, Integer::sum));

        result.put("joinedNames", joinedNames);
        Map<String, Object> ageStatsMap = new java.util.HashMap<>();
        ageStatsMap.put("count", ageStats.getCount());
        ageStatsMap.put("sum", ageStats.getSum());
        ageStatsMap.put("min", ageStats.getMin());
        ageStatsMap.put("average", String.format("%.2f", ageStats.getAverage()));
        ageStatsMap.put("max", ageStats.getMax());
        result.put("ageStats", ageStatsMap);
        result.put("maxSalaryEmployee", maxSalary.map(e -> e.getName() + ":" + e.getSalary()).orElse("N/A"));
        result.put("totalSalary", totalSalary);
        result.put("interviewNote", "joining 做字符串拼接，summarizingInt 一次性统计，maxBy/reducing 做聚合；reducing 必须满足结合律才能在并行流下正确工作。");
        return result;
    }
}
