package com.example.mp.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mp.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper：继承 BaseMapper 获得基础 CRUD。
 *
 * 面试点：
 * - BaseMapper<T> 提供了 insert、deleteById、deleteByMap、deleteBatchIds、updateById、selectById、selectBatchIds、selectByMap、selectOne、selectCount、selectList、selectMaps、selectObjs、selectPage 等方法。
 * - 自定义 SQL 与 Wrapper 查询可以共存。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 自定义 SQL：查询用户及其订单金额汇总（用于实战演示）。
     */
    @Select("SELECT u.id, u.username, SUM(o.amount) as totalAmount, COUNT(o.id) as orderCount " +
            "FROM t_user u LEFT JOIN t_order o ON u.id = o.user_id " +
            "WHERE u.deleted = 0 " +
            "GROUP BY u.id, u.username")
    List<Map<String, Object>> selectUserOrderStats();

    /**
     * 按状态统计用户人数与平均年龄。
     */
    @Select("SELECT status, COUNT(*) as cnt, AVG(age) as avgAge FROM t_user WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> selectStatusStats();

    /**
     * 根据用户名模糊查询。
     */
    @Select("SELECT * FROM t_user WHERE deleted = 0 AND username LIKE CONCAT('%', #{name}, '%')")
    List<User> selectByNameLike(@Param("name") String name);

    /**
     * 演示 @InterceptorIgnore：临时关闭防止全表删除插件。
     * 调用后由业务层事务回滚，避免真实删除数据。
     */
    @InterceptorIgnore(blockAttack = "true")
    @Delete("DELETE FROM t_user")
    int deleteAllForDemo();

    /**
     * 自定义 SQL 注入器 InsertBatchSomeColumn 提供实现：
     * 生成 INSERT INTO t_user (...) VALUES (...),(...),(...) 一条 SQL。
     */
    int insertBatchSomeColumn(List<User> list);

    /**
     * 复杂 Wrapper 演示：按状态分组并过滤用户数大于指定值的状态。
     * 使用自定义 SQL 绕过 User 实体上的 @OrderBy 默认排序。
     */
    @Select("SELECT status, COUNT(*) as cnt FROM t_user WHERE deleted = 0 GROUP BY status HAVING COUNT(*) > #{minCount}")
    List<Map<String, Object>> selectStatusHaving(@Param("minCount") int minCount);
}
