package com.example.mp.more;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mp.entity.accessors.ChainUser;
import com.example.mp.entity.accessors.FluentUser;
import com.example.mp.entity.accessors.PrefixUser;
import com.example.mp.entity.Account;
import com.example.mp.entity.Product;
import com.example.mp.entity.Task;
import com.example.mp.entity.User;
import com.example.mp.mapper.AccountMapper;
import com.example.mp.mapper.ProductMapper;
import com.example.mp.mapper.UserMapper;
import com.example.mp.order.StartupOrderRecorder;
import com.example.mp.service.TaskService;
import com.example.mp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 更多 MyBatis-Plus 注解场景服务。
 */
@Service
@RequiredArgsConstructor
public class MoreAnnotationService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final AccountMapper accountMapper;
    private final ProductMapper productMapper;
    private final TaskService taskService;
    private final StartupOrderRecorder startupOrderRecorder;

    /**
     * @KeySequence 演示：H2 序列生成主键。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> keySequenceDemo() {
        Map<String, Object> result = new HashMap<>();
        Product product = new Product();
        product.setName("序列商品-" + System.currentTimeMillis());
        product.setPrice(new BigDecimal("199.99"));

        int rows = productMapper.insert(product);

        result.put("insertRows", rows);
        result.put("filledId", product.getId());
        result.put("note", "@KeySequence(value=\"seq_product\", dbType=DbType.H2) + @TableId(type=IdType.INPUT) 让 H2 序列生成主键并回填");
        return result;
    }

    /**
     * @OrderBy 演示：User.createTime 标注 @OrderBy 后，list 自动按创建时间倒序。
     */
    public Map<String, Object> orderByDemo() {
        Map<String, Object> result = new HashMap<>();

        // 查询全部，观察是否按 create_time 排序
        List<User> users = userService.list();
        List<Long> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(user.getId());
        }

        result.put("userIdsOrder", ids);
        result.put("note", "@OrderBy(asc=false) 在实体字段上标注，list()/lambdaQuery() 等查询会自动追加 ORDER BY");
        return result;
    }

    /**
     * @EnumValue 演示：gender 字段按枚举 code 存储，按 desc 展示。
     */
    public Map<String, Object> enumValueDemo() {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.selectById(1001L);

        result.put("user", user);
        result.put("genderCode", user.getGender() != null ? user.getGender().getCode() : null);
        result.put("genderDesc", user.getGender() != null ? user.getGender().getDesc() : null);
        result.put("note", "@EnumValue 标记持久化到数据库的字段；@JsonValue 控制返回前端的值");
        return result;
    }

    /**
     * @InterceptorIgnore 演示：临时关闭防止全表删除插件。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> interceptorIgnoreDemo() {
        Map<String, Object> result = new HashMap<>();

        long before = userService.count();
        // 无 @InterceptorIgnore 时，BlockAttackInnerInterceptor 会阻止无 WHERE 的 DELETE
        int deleted = userMapper.deleteAllForDemo();

        result.put("beforeCount", before);
        result.put("deletedRows", deleted);
        result.put("note", "@InterceptorIgnore(blockAttack=\"true\") 临时关闭防止全表更新/删除插件；事务已回滚，未真实删除");

        // 事务回滚，避免影响其他场景
        org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }

    /**
     * @TableField(select = false) 演示：查询 Account 时 password 字段不会返回。
     */
    public Map<String, Object> fieldSelectDemo() {
        Map<String, Object> result = new HashMap<>();
        Account account = accountMapper.selectById(3001L);

        result.put("account", account);
        result.put("passwordIsNull", account.getPassword() == null);
        result.put("note", "@TableField(select = false) 的字段不会出现在 SELECT 列表，适合密码等敏感字段");
        return result;
    }

    /**
     * @TableField(condition) 演示：自定义 LIKE 条件模板。
     */
    public Map<String, Object> fieldConditionDemo() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<Account> wrapper = new QueryWrapper<>();
        wrapper.eq("email", "example");
        List<Account> accounts = accountMapper.selectList(wrapper);

        result.put("conditionValue", "example");
        result.put("accounts", accounts);
        result.put("note", "@TableField(condition=\"%s LIKE CONCAT('%%',#{%s},'%%')\") 让 eq 也走 LIKE");
        return result;
    }

    /**
     * @TableField(update) 演示：自定义 SET 片段，每次更新 login_count 自动 +1。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> fieldUpdateDemo() {
        Map<String, Object> result = new HashMap<>();

        Account account = accountMapper.selectById(3001L);
        Integer oldCount = account.getLoginCount();

        // 只更新 login_count，SQL 会变成 login_count = login_count + 1
        account.setLoginCount(null); // 清空，让 MP 只根据 @TableField(update) 生成 SET
        accountMapper.updateById(account);

        Account updated = accountMapper.selectById(3001L);

        result.put("oldCount", oldCount);
        result.put("newCount", updated.getLoginCount());
        result.put("note", "@TableField(update=\"%s+1\") 可生成自增/自定义 SET 片段");
        return result;
    }

    /**
     * @TableField(numericScale) 演示：DECIMAL 精度。
     */
    public Map<String, Object> fieldNumericScaleDemo() {
        Map<String, Object> result = new HashMap<>();
        Account account = accountMapper.selectById(3001L);

        result.put("account", account);
        result.put("balance", account.getBalance());
        result.put("note", "@TableField(numericScale=\"2\") 指定 BigDecimal 保留两位小数");
        return result;
    }

    /**
     * IdType 全策略一览。
     */
    public Map<String, Object> idTypesDemo() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> strategies = new LinkedHashMap<>();
        strategies.put("AUTO", "数据库自增");
        strategies.put("INPUT", "外部传入，常与 @KeySequence 配合");
        strategies.put("ASSIGN_ID", "雪花算法，Long 型");
        strategies.put("ASSIGN_UUID", "UUID 字符串");
        strategies.put("NONE", "不生成主键");

        result.put("idTypes", strategies);
        result.put("note", "ASSIGN_ID 是 MP 推荐的全局唯一主键策略");
        return result;
    }

    /**
     * FieldStrategy 演示：NOT_NULL / NOT_EMPTY / IGNORED / DEFAULT。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> fieldStrategyDemo() {
        Map<String, Object> result = new HashMap<>();

        // 创建 Account，tag 为空字符串
        Account account = new Account();
        account.setUsername("strategy-demo-" + System.currentTimeMillis());
        account.setPassword("pwd");
        account.setEmail("strategy@example.com");
        account.setBalance(new BigDecimal("100.50"));

        accountMapper.insert(account);

        result.put("insertedId", account.getId());
        result.put("strategies", Map.of(
                "DEFAULT", "继承全局配置",
                "NOT_NULL", "字段非 null 才参与 SQL",
                "NOT_EMPTY", "字段非 null 且非空字符串才参与 SQL",
                "IGNORED", "字段永远参与 SQL，即使为 null"
        ));
        result.put("note", "可通过 @TableField(insertStrategy/updateStrategy) 或全局配置控制 null/空值行为");
        return result;
    }

    /**
     * @Order 演示：返回 CommandLineRunner 的启动执行顺序。
     */
    public Map<String, Object> orderDemo() {
        Map<String, Object> result = new HashMap<>();
        result.put("startupOrder", StartupOrderRecorder.getLogs());
        result.put("note", "@Order 数字越小优先级越高；未标注 @Order 的 Bean 默认 Integer.MAX_VALUE，最后执行");
        return result;
    }

    /**
     * 自定义注解 + AOP 演示：@AutoFillUser 自动填充 createBy / updateBy。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> customAnnotationDemo() {
        Map<String, Object> result = new HashMap<>();

        // 1) 创建任务：createBy / updateBy 由 AOP 自动填充
        Task created = taskService.createTask("自定义注解+AOP 演示任务");

        // 2) 完成任务：updateBy 由 AOP 自动填充
        Task finished = taskService.finishTask(created.getId());

        result.put("createdTask", created);
        result.put("finishedTask", finished);
        result.put("note", "@AutoFillUser + AOP 自动填充操作人；ValidationAspect(@Order=10) 先执行，AutoFillUserAspect(@Order=20) 后执行");
        return result;
    }

    /**
     * @Accessors 演示：chain / fluent / prefix。
     */
    public Map<String, Object> accessorsDemo() {
        Map<String, Object> result = new HashMap<>();

        ChainUser chainUser = new ChainUser()
                .setId(1L)
                .setName("chain-user")
                .setAge(18);

        FluentUser fluentUser = new FluentUser()
                .id(2L)
                .name("fluent-user");

        PrefixUser prefixUser = new PrefixUser()
                .setName("prefix-user")
                .setAge(20);

        result.put("chainUser", chainUser);
        result.put("fluentUser", fluentUser);
        result.put("prefixUser", prefixUser);
        result.put("note", "@Accessors(chain=true) 返回 this 支持链式调用；fluent=true 省略 get/set 前缀；prefix 可剥离字段前缀");
        return result;
    }
}
