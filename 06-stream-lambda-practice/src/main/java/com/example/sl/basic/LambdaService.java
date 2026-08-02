package com.example.sl.basic;

import com.example.sl.domain.Employee;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class LambdaService {

    public Map<String, Object> functionalDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Employee> sample = Arrays.asList(
                new Employee(1L, "Alice", 25, "研发", 12000, "女", java.time.LocalDate.of(2020, 3, 1)),
                new Employee(2L, "Bob", 30, "测试", 9000, "男", java.time.LocalDate.of(2019, 6, 15)),
                new Employee(3L, "Carol", 35, "研发", 18000, "女", java.time.LocalDate.of(2018, 1, 10))
        );

        Predicate<Employee> highSalary = e -> e.getSalary() > 10000;
        Function<Employee, String> nameAndDept = e -> e.getName() + "@" + e.getDept();
        Consumer<Employee> printName = e -> System.out.println(e.getName());
        Supplier<Employee> defaultEmployee = () -> new Employee(0L, "Default", 0, "未知", 0, "未知", java.time.LocalDate.now());

        List<String> highSalaryNames = sample.stream()
                .filter(highSalary)
                .map(nameAndDept)
                .collect(Collectors.toList());

        List<String> consumedNames = new ArrayList<>();
        Consumer<Employee> collectName = e -> consumedNames.add(e.getName());
        sample.forEach(collectName);

        Employee supplied = defaultEmployee.get();

        result.put("highSalaryNames", highSalaryNames);
        result.put("consumedNames", consumedNames);
        result.put("suppliedEmployee", supplied);
        result.put("interviewNote", "函数式接口只有一个抽象方法：Predicate<T> 返回 boolean，Function<T,R> 做映射，Consumer<T> 消费，Supplier<T> 供给。");
        return result;
    }

    public Map<String, Object> methodRefDemo() {
        Map<String, Object> result = new HashMap<>();

        List<Employee> sample = Arrays.asList(
                new Employee(1L, "Alice", 25, "研发", 12000, "女", java.time.LocalDate.of(2020, 3, 1)),
                new Employee(2L, "Bob", 30, "测试", 9000, "男", java.time.LocalDate.of(2019, 6, 15))
        );

        List<String> instanceNames = sample.stream()
                .map(Employee::getName)
                .collect(Collectors.toList());

        List<String> stringValues = sample.stream()
                .map(Employee::getSalary)
                .map(String::valueOf)
                .collect(Collectors.toList());

        List<Employee> constructed = Arrays.asList("Dave", "Eve").stream()
                .map(name -> new Employee(null, name, 28, "产品", 10000, "男", java.time.LocalDate.of(2021, 1, 1)))
                .collect(Collectors.toList());

        result.put("instanceMethodRef", instanceNames);
        result.put("staticMethodRef", stringValues);
        result.put("constructorRef", constructed.stream().map(Employee::getName).collect(Collectors.toList()));
        result.put("interviewNote", "方法引用是 Lambda 的简写：对象::实例方法、类::静态方法、类::实例方法、类::new，要求函数式接口参数/返回值匹配。");
        return result;
    }
}
