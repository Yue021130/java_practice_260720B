package com.example.caa.support;

import com.example.caa.domain.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内存数据仓库：模拟 DAO 层。
 */
@Component
public class MockDataRepository {

    private final List<User> users = new ArrayList<>();

    @PostConstruct
    public void init() {
        users.add(User.builder().id(1L).name("张三").phone("13800138001").email("zhangsan@example.com").idCard("110101199001011234").build());
        users.add(User.builder().id(2L).name("李四").phone("13900139002").email("lisi@example.com").idCard("110101199002021234").build());
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public User findById(Long id) {
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
