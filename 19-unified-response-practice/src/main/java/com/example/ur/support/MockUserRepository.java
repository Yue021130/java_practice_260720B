package com.example.ur.support;

import com.example.ur.domain.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存用户仓库：模拟 DAO 层。
 *
 * <p>不需要 Redis / 数据库，启动时初始化一批数据，支持增删改查与分页。</p>
 */
@Component
public class MockUserRepository {

    private final List<User> users = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idGenerator = new AtomicLong(100);

    @PostConstruct
    public void init() {
        users.add(User.builder().id(idGenerator.incrementAndGet()).name("张三").age(28).email("zhangsan@example.com").phone("13800138001").password("123456").build());
        users.add(User.builder().id(idGenerator.incrementAndGet()).name("李四").age(35).email("lisi@example.com").phone("13900139002").password("123456").build());
        users.add(User.builder().id(idGenerator.incrementAndGet()).name("王五").age(22).email("wangwu@example.com").phone("13700137003").password("123456").build());
        users.add(User.builder().id(idGenerator.incrementAndGet()).name("赵六").age(30).email("zhaoliu@example.com").phone("13600136004").password("123456").build());
        users.add(User.builder().id(idGenerator.incrementAndGet()).name("孙七").age(26).email("sunqi@example.com").phone("13500135005").password("123456").build());
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public Optional<User> findById(Long id) {
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.incrementAndGet());
        }
        users.add(user);
        return user;
    }

    public boolean update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                return true;
            }
        }
        return false;
    }

    public boolean deleteById(Long id) {
        return users.removeIf(u -> u.getId().equals(id));
    }

    /**
     * 内存分页：模拟 MyBatis-Plus 的 Page 行为。
     */
    public PageData<User> page(long pageNum, long pageSize) {
        long total = users.size();
        long fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= total) {
            return new PageData<>(Collections.emptyList(), total, pageNum, pageSize);
        }
        long toIndex = Math.min(fromIndex + pageSize, total);
        List<User> list = new ArrayList<>(users.subList((int) fromIndex, (int) toIndex));
        return new PageData<>(list, total, pageNum, pageSize);
    }

    /**
     * 简单分页数据载体，避免直接引入 MyBatis-Plus 依赖。
     */
    public static class PageData<T> {
        private final List<T> list;
        private final long total;
        private final long pageNum;
        private final long pageSize;

        public PageData(List<T> list, long total, long pageNum, long pageSize) {
            this.list = list;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getList() { return list; }
        public long getTotal() { return total; }
        public long getPageNum() { return pageNum; }
        public long getPageSize() { return pageSize; }
    }
}
