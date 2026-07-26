package com.example.mp.extension;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.mp.entity.Article;
import com.example.mp.entity.Report;
import com.example.mp.entity.User;
import com.example.mp.mapper.ArticleMapper;
import com.example.mp.mapper.ReportMapper;
import com.example.mp.mapper.UserMapper;
import com.example.mp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * MyBatis-Plus 扩展能力演示服务。
 */
@Service
@RequiredArgsConstructor
public class ExtensionService {

    private final ArticleMapper articleMapper;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    /**
     * TypeHandler 演示：Map 字段以 JSON 形式存库并读取。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> typeHandlerDemo() {
        Map<String, Object> result = new HashMap<>();

        Article article = new Article();
        article.setTitle("TypeHandler 演示文章");
        article.setContent("演示 JacksonTypeHandler 把 Map 序列化成 JSON。");
        article.setExtra(Map.of("author", "mp", "tag", "tutorial", "pv", 100));
        article.setCreateTime(LocalDateTime.now());

        articleMapper.insert(article);
        Article db = articleMapper.selectById(article.getId());

        result.put("insertedId", article.getId());
        result.put("dbExtra", db.getExtra());
        result.put("note", "@TableField(typeHandler = JacksonTypeHandler.class) 让 Map/List 等对象与 JSON 字符串自动互转");
        return result;
    }

    /**
     * ActiveRecord 模式演示：实体继承 Model 后直接调用 insert/selectById。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activeRecordDemo() {
        Map<String, Object> result = new HashMap<>();

        Article article = new Article();
        article.setTitle("ActiveRecord 演示");
        article.setContent("extends Model<T> 后可直接 article.insert()。");
        article.setExtra(Map.of("source", "AR"));
        article.setCreateTime(LocalDateTime.now());

        // ActiveRecord 插入
        article.insert();

        // ActiveRecord 查询（Model 的 selectById 是实例方法，用当前对象携带的泛型类型信息）
        Article db = article.selectById(article.getId());

        result.put("insertedId", article.getId());
        result.put("dbTitle", db != null ? db.getTitle() : null);
        result.put("note", "实体 extends Model<T> 后，无需注入 Mapper/Service 即可进行单表 CRUD");
        return result;
    }

    /**
     * 动态表名演示：按月分表插入并查询。
     */
    public Map<String, Object> dynamicTableNameDemo() {
        Map<String, Object> result = new HashMap<>();
        Map<String, List<Report>> records = new LinkedHashMap<>();

        try {
            // 写入 2024-01 分表
            TableNameContext.set("202401");
            Report jan = new Report();
            jan.setReportMonth("2024-01");
            jan.setContent("1 月报表数据");
            reportMapper.insert(jan);
            records.put("202401", reportMapper.selectList(null));

            // 写入 2024-02 分表
            TableNameContext.set("202402");
            Report feb = new Report();
            feb.setReportMonth("2024-02");
            feb.setContent("2 月报表数据");
            reportMapper.insert(feb);
            records.put("202402", reportMapper.selectList(null));
        } finally {
            TableNameContext.clear();
        }

        result.put("records", records);
        result.put("note", "DynamicTableNameInnerInterceptor 根据上下文把逻辑表 t_report 替换为 t_report_202401 / t_report_202402");
        return result;
    }

    /**
     * InsertBatchSomeColumn 演示：一条 SQL 批量插入。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> insertBatchSomeColumnDemo() {
        Map<String, Object> result = new HashMap<>();

        List<User> users = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            User user = new User();
            user.setUsername("batch-" + ts + "-" + i);
            user.setAge(20 + i);
            user.setEmail("batch" + i + "@example.com");
            user.setStatus(1);
            users.add(user);
        }

        // CustomSqlInjector 为所有 Mapper 注入了 insertBatchSomeColumn 方法
        int rows = userMapper.insertBatchSomeColumn(users);
        List<Long> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(user.getId());
        }

        result.put("insertRows", rows);
        result.put("ids", ids);
        result.put("note", "insertBatchSomeColumn 通过一条 INSERT ... VALUES (...),(...) 实现批量插入，性能远高于循环单条 insert");
        return result;
    }

    /**
     * 链式 Wrapper 演示：lambdaQuery / lambdaUpdate 一行链式调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> chainWrapperDemo() {
        Map<String, Object> result = new HashMap<>();

        // 链式查询
        List<User> list = userService.lambdaQuery()
                .eq(User::getStatus, 1)
                .ge(User::getAge, 20)
                .list();

        // 链式更新：把 user_id=1006 的年龄改成 31
        boolean updated = userService.lambdaUpdate()
                .set(User::getAge, 31)
                .eq(User::getId, 1006L)
                .update();

        result.put("queryCount", list.size());
        result.put("updated", updated);
        result.put("note", "IService.lambdaQuery()/lambdaUpdate() 返回链式 Wrapper，无需手动 new QueryWrapper/UpdateWrapper");

        // 回滚更新，避免影响其他场景
        org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }

    /**
     * 复杂 Wrapper 演示：inSql / groupBy having。
     */
    public Map<String, Object> wrapperAdvancedDemo() {
        Map<String, Object> result = new HashMap<>();

        // inSql：查询「订单金额 > 100」的用户
        QueryWrapper<User> inSqlWrapper = new QueryWrapper<>();
        inSqlWrapper.inSql("id", "select user_id from t_order where amount > 100");
        List<User> highValueUsers = userMapper.selectList(inSqlWrapper);

        // groupBy + having：按状态分组，只保留用户数 > 1 的状态
        // 用自定义 SQL 演示，避免 User 实体 @OrderBy 自动追加排序导致 H2 语法错误
        List<Map<String, Object>> statusCount = userMapper.selectStatusHaving(1);

        result.put("highValueUserIds", highValueUsers.stream().map(User::getId).toList());
        result.put("statusCount", statusCount);
        result.put("note", "inSql 适合子查询；groupBy + having 适合聚合统计");
        return result;
    }

    /**
     * selectMaps / selectObjs 演示：返回 Map 或单列集合。
     */
    public Map<String, Object> selectMapsDemo() {
        Map<String, Object> result = new HashMap<>();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("id", "username", "age")
                .last("limit 5");
        List<Map<String, Object>> maps = userMapper.selectMaps(wrapper);

        List<Object> ids = userMapper.selectObjs(
                new QueryWrapper<User>().select("id").last("limit 5")
        );

        result.put("maps", maps);
        result.put("ids", ids);
        result.put("note", "selectMaps 返回 List<Map>，selectObjs 返回单列 List<Object>，常用于报表或下拉框");
        return result;
    }

    /**
     * Wrapper 更新 / 删除演示。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> wrapperUpdateDeleteDemo() {
        Map<String, Object> result = new HashMap<>();

        // Wrapper 批量更新：把 status=0 的用户改为 status=1
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("status", 0).set("status", 1);
        int updateRows = userMapper.update(null, updateWrapper);

        // Wrapper 删除：先插一条临时数据，再按条件删除
        User temp = new User();
        temp.setUsername("temp-delete-user");
        temp.setAge(99);
        temp.setEmail("temp@example.com");
        temp.setStatus(1);
        userMapper.insert(temp);

        int deletedRows = userMapper.delete(
                new QueryWrapper<User>().eq("id", temp.getId())
        );

        result.put("updateRows", updateRows);
        result.put("deletedRows", deletedRows);
        result.put("note", "update(entity, wrapper) 中 entity 为 null 时只按 wrapper 生成 SET；delete(wrapper) 按条件删除");

        // 回滚，避免影响其他场景
        org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
    }
}
