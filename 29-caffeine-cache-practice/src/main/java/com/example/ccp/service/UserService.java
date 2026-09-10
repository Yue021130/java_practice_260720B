package com.example.ccp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ccp.common.BusinessException;
import com.example.ccp.dto.UserSaveDTO;
import com.example.ccp.dto.UserVO;
import com.example.ccp.entity.User;
import com.example.ccp.mapper.UserMapper;
import com.example.ccp.util.PasswordUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户 Service：CRUD + Caffeine 缓存实战。
 *
 * <p>缓存设计（对应 Caffeine 文档的缓存一致性原则——先更新数据库，再删缓存）：</p>
 * <ul>
 *   <li>{@link #page} / {@link #getById} 加 {@code @Cacheable}，命中时控制台不再打印 SQL；</li>
 *   <li>新增/修改/删除统一 {@code @CacheEvict(allEntries = true)} 清空 user、userPage 两个缓存，
 *       保证后续读取拿到的都是最新数据；</li>
 *   <li>演示方法：连续两次调用 getById，第一次走库、第二次走缓存，可在 /api/cache/stats 看命中率。</li>
 * </ul>
 */
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private PasswordUtil passwordUtil;

    /**
     * 分页查询。key 由 页码+每页条数+用户名 组成；条件变了就是另一个缓存条目。
     */
    @Cacheable(cacheNames = "userPage", key = "#current + ':' + #size + ':' + (#username == null ? '' : #username)")
    public Page<UserVO> page(long current, long size, String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(username != null && !username.isEmpty(), User::getUsername, username)
                .orderByDesc(User::getCreateTime);
        Page<User> entityPage = userMapper.selectPage(new Page<>(current, size), wrapper);
        // 实体转 VO，剔除 password
        Page<UserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(java.util.stream.Collectors.toList()));
        return voPage;
    }

    /**
     * 按 id 查询（缓存演示的核心方法）。unless 防止把 null 缓存结果误导后续判断。
     */
    @Cacheable(cacheNames = "user", key = "#id", unless = "#result == null")
    public UserVO getById(Long id) {
        return toVO(userMapper.selectById(id));
    }

    /**
     * 新增用户；写库成功后清空相关缓存（先写库、后清缓存，避免脏读窗口）。
     */
    @CacheEvict(cacheNames = {"user", "userPage"}, allEntries = true)
    public UserVO save(UserSaveDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setId(null);
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userMapper.insert(user);
        return toVO(user);
    }

    /**
     * 编辑用户；密码留空表示不修改。
     */
    @CacheEvict(cacheNames = {"user", "userPage"}, allEntries = true)
    public UserVO update(UserSaveDTO dto) {
        User user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        if (!user.getUsername().equals(dto.getUsername())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.getUsername()).ne(User::getId, dto.getId()));
            if (count > 0) {
                throw new BusinessException(400, "用户名已存在");
            }
        }
        BeanUtils.copyProperties(dto, user);
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordUtil.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
        return toVO(userMapper.selectById(dto.getId()));
    }

    /**
     * 逻辑删除（@TableLogic 自动置 deleted=1），并清空缓存。
     */
    @CacheEvict(cacheNames = {"user", "userPage"}, allEntries = true)
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    private UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
