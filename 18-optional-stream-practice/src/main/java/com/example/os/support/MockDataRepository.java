package com.example.os.support;

import com.example.os.domain.*;
import com.example.os.domain.Order.OrderStatus;
import com.example.os.domain.User.UserLevel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 内存 Mock 数据仓库：模拟 DAO 层，所有业务场景都从这里取数据。
 * 不需要 Redis / 数据库，mvn spring-boot:run 即可运行。
 */
@Component
public class MockDataRepository {

    private final List<User> users = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();
    private final List<Sku> skus = new ArrayList<>();
    private final List<Menu> menus = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final List<ImportRow> importRows = new ArrayList<>();

    @PostConstruct
    public void init() {
        initUsers();
        initOrders();
        initProductsAndSkus();
        initMenus();
        initNotifications();
        initImportRows();
    }

    private void initUsers() {
        users.add(User.builder().id(1L).name("张三").email("zhangsan@example.com").phone("13800138001").level(UserLevel.VIP).tags(Arrays.asList("高频", "数码")).build());
        users.add(User.builder().id(2L).name("李四").email(null).phone("13900139002").level(UserLevel.NORMAL).tags(Arrays.asList("新人")).build());
        users.add(User.builder().id(3L).name("王五").email("wangwu@example.com").phone(null).level(UserLevel.VIP).tags(Arrays.asList("美妆", "高频")).build());
        users.add(User.builder().id(4L).name("赵六").email("zhaoliu@example.com").phone("13700137004").level(UserLevel.GUEST).tags(Collections.emptyList()).build());
        users.add(User.builder().id(5L).name("孙七").email("sunqi@example.com").phone("13600136005").level(UserLevel.NORMAL).tags(Arrays.asList("图书")).build());
    }

    private void initOrders() {
        LocalDateTime now = LocalDateTime.now();
        orders.add(Order.builder().id(101L).userId(1L).amount(new BigDecimal("299.00")).status(OrderStatus.COMPLETED).createTime(now.minusDays(2)).build());
        orders.add(Order.builder().id(102L).userId(1L).amount(new BigDecimal("1999.00")).status(OrderStatus.PAID).createTime(now.minusDays(5)).build());
        orders.add(Order.builder().id(103L).userId(2L).amount(new BigDecimal("59.90")).status(OrderStatus.CANCELLED).createTime(now.minusDays(10)).build());
        orders.add(Order.builder().id(104L).userId(3L).amount(new BigDecimal("899.00")).status(OrderStatus.SHIPPED).createTime(now.minusDays(1)).build());
        orders.add(Order.builder().id(105L).userId(3L).amount(new BigDecimal("3999.00")).status(OrderStatus.COMPLETED).createTime(now.minusDays(15)).build());
        orders.add(Order.builder().id(106L).userId(5L).amount(new BigDecimal("128.00")).status(OrderStatus.COMPLETED).createTime(now.minusDays(20)).build());
        orders.add(Order.builder().id(107L).userId(1L).amount(new BigDecimal("999.00")).status(OrderStatus.COMPLETED).createTime(now.minusDays(35)).build());
    }

    private void initProductsAndSkus() {
        products.add(Product.builder().id(1L).name("iPhone 15").category("数码").build());
        products.add(Product.builder().id(2L).name("戴森吹风机").category("家电").build());

        skus.add(Sku.builder().id(11L).productId(1L).skuCode("IPHONE15-128-BLACK").skuName("128G 黑色").price(new BigDecimal("5999.00")).stock(100).enabled(true).build());
        skus.add(Sku.builder().id(12L).productId(1L).skuCode("IPHONE15-256-WHITE").skuName("256G 白色").price(new BigDecimal("6999.00")).stock(50).enabled(true).build());
        skus.add(Sku.builder().id(13L).productId(1L).skuCode("IPHONE15-512-BLUE").skuName("512G 蓝色").price(null).stock(0).enabled(false).build());
        skus.add(Sku.builder().id(21L).productId(2L).skuCode("DYSON-HD15").skuName("HD15 紫红色").price(new BigDecimal("2999.00")).stock(20).enabled(true).build());
    }

    private void initMenus() {
        // 根菜单
        menus.add(Menu.builder().id(1L).parentId(0L).name("系统管理").code("sys").orderNum(1).build());
        menus.add(Menu.builder().id(2L).parentId(0L).name("订单管理").code("order").orderNum(2).build());
        // 系统管理子菜单
        menus.add(Menu.builder().id(11L).parentId(1L).name("用户管理").code("sys:user").orderNum(1).build());
        menus.add(Menu.builder().id(12L).parentId(1L).name("角色管理").code("sys:role").orderNum(2).build());
        // 用户管理子菜单
        menus.add(Menu.builder().id(111L).parentId(11L).name("用户列表").code("sys:user:list").orderNum(1).build());
        // 订单管理子菜单
        menus.add(Menu.builder().id(21L).parentId(2L).name("订单列表").code("order:list").orderNum(1).build());
    }

    private void initNotifications() {
        LocalDateTime now = LocalDateTime.now();
        notifications.add(Notification.builder().id(1L).userId(1L).type("PROMOTION").title("会员日全场 8 折").read(false).createTime(now.minusHours(2)).build());
        notifications.add(Notification.builder().id(2L).userId(1L).type("ORDER").title("您的订单已发货").read(true).createTime(now.minusDays(1)).build());
        notifications.add(Notification.builder().id(3L).userId(2L).type(null).title(null).read(false).createTime(now.minusDays(10)).build());
        notifications.add(Notification.builder().id(4L).userId(3L).type("PROMOTION").title("新品上市").read(false).createTime(now.minusHours(5)).build());
    }

    private void initImportRows() {
        importRows.add(ImportRow.builder().rowNum(1).name("张三").age("28").email("zs@example.com").phone("13800138001").amount("199.50").build());
        importRows.add(ImportRow.builder().rowNum(2).name(null).age("abc").email("invalid").phone(null).amount("").build());
        importRows.add(ImportRow.builder().rowNum(3).name("李四").age("35").email("ls@example.com").phone("13900139002").amount("2999.00").build());
        importRows.add(ImportRow.builder().rowNum(4).name("王五").age("").email(null).phone("13700137004").amount("-100").build());
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public List<Sku> getSkus() {
        return Collections.unmodifiableList(skus);
    }

    public List<Menu> getMenus() {
        return Collections.unmodifiableList(menus);
    }

    public List<Notification> getNotifications() {
        return Collections.unmodifiableList(notifications);
    }

    public List<ImportRow> getImportRows() {
        return Collections.unmodifiableList(importRows);
    }

    /**
     * 按 ID 查询用户，返回 Optional：避免返回 null，调用方可链式处理。
     */
    public java.util.Optional<User> findUserById(Long id) {
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    /**
     * 按用户 ID 查询订单列表，可能为空列表，但绝不会返回 null。
     */
    public List<Order> findOrdersByUserId(Long userId) {
        return orders.stream()
                .filter(o -> o.getUserId().equals(userId))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 按 ID 查询商品，返回 Optional：用于 SKU 价格场景的安全解包。
     */
    public java.util.Optional<Product> findProductById(Long id) {
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }
}
