package com.example.sl.data;

import com.example.sl.domain.Employee;
import com.example.sl.domain.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockDataGenerator {

    private static final int EMPLOYEE_COUNT = 20000;
    private static final int ORDER_COUNT = 20000;

    private final List<Employee> employees = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();

    private static final String[] DEPTS = {"研发", "测试", "产品", "运维", "设计", "市场", "财务", "人事"};
    private static final String[] GENDERS = {"男", "女"};
    private static final String[] ORDER_STATUS = {"PAID", "UNPAID", "SHIPPED", "COMPLETED", "CANCELLED"};
    private static final String[] FIRST_NAMES = {"伟", "芳", "娜", "敏", "静", "强", "磊", "洋", "勇", "艳"};
    private static final String[] LAST_NAMES = {"张", "王", "李", "刘", "陈", "杨", "黄", "赵", "吴", "周"};

    @PostConstruct
    public void init() {
        Random random = ThreadLocalRandom.current();
        LocalDate baseDate = LocalDate.of(2018, 1, 1);
        LocalDateTime baseTime = LocalDateTime.of(2023, 1, 1, 0, 0);

        for (long i = 1; i <= EMPLOYEE_COUNT; i++) {
            Employee e = new Employee();
            e.setId(i);
            e.setName(LAST_NAMES[random.nextInt(LAST_NAMES.length)]
                    + FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]
                    + (random.nextBoolean() ? "" : FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]));
            e.setAge(22 + random.nextInt(38));
            e.setDept(DEPTS[random.nextInt(DEPTS.length)]);
            e.setSalary(5000 + random.nextInt(35000));
            e.setGender(GENDERS[random.nextInt(GENDERS.length)]);
            e.setJoinDate(baseDate.plusDays(random.nextInt(2000)));
            employees.add(e);
        }

        for (long i = 1; i <= ORDER_COUNT; i++) {
            Order o = new Order();
            o.setId(i);
            o.setUserId(1000L + random.nextInt(9000));
            o.setAmount(BigDecimal.valueOf(10 + random.nextDouble() * 4990).setScale(2, RoundingMode.HALF_UP));
            o.setStatus(ORDER_STATUS[random.nextInt(ORDER_STATUS.length)]);
            o.setCreateTime(baseTime.plusSeconds(random.nextInt(31_536_000)));
            orders.add(o);
        }
    }

    public List<Employee> getEmployees() {
        return Collections.unmodifiableList(employees);
    }

    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public int getEmployeeCount() {
        return employees.size();
    }

    public int getOrderCount() {
        return orders.size();
    }
}
