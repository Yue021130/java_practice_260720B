package com.example.ur.user;

import com.example.ur.common.result.BusinessException;
import com.example.ur.common.result.PageResult;
import com.example.ur.common.result.Result;
import com.example.ur.common.result.ResultCode;
import com.example.ur.common.result.ResultFactory;
import com.example.ur.domain.User;
import com.example.ur.support.MockUserRepository;
import com.example.ur.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑层。
 *
 * <p>负责实体转 VO、业务校验、主动抛 BusinessException。
 * Controller 里大部分接口直接返回裸对象/List/PageResult，由 GlobalResponseAdvice 自动包装。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final MockUserRepository userRepository;

    /**
     * 根据 ID 查询用户。
     */
    public UserVO getById(Long id) {
        return userRepository.findById(id)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在: " + id));
    }

    /**
     * 查询全部用户。
     */
    public List<UserVO> list() {
        return userRepository.findAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询用户。
     *
     * <p>这里在 Service 层返回 PageResult，Controller 可以选择：
     * 1. 直接返回 PageResult，由 GlobalResponseAdvice 包装成 Result&lt;PageResult&lt;UserVO&gt;&gt;；
     * 2. 手动调用 ResultFactory.success(pageResult) 返回 Result。</p>
     */
    public PageResult<UserVO> page(long pageNum, long pageSize) {
        MockUserRepository.PageData<User> pageData = userRepository.page(pageNum, pageSize);
        List<UserVO> voList = pageData.getList().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, pageData.getTotal(), pageData.getPageNum(), pageData.getPageSize());
    }

    /**
     * 创建用户。
     */
    public UserVO create(User user) {
        User saved = userRepository.save(user);
        return toVO(saved);
    }

    /**
     * 更新用户。
     */
    public UserVO update(User user) {
        boolean updated = userRepository.update(user);
        if (!updated) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在: " + user.getId());
        }
        return toVO(user);
    }

    /**
     * 删除用户。
     */
    public boolean delete(Long id) {
        boolean deleted = userRepository.deleteById(id);
        if (!deleted) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在: " + id);
        }
        return true;
    }

    /**
     * 模拟直接返回 Result：某些场景下 Controller 需要手动包装，
     * GlobalResponseAdvice 会识别出已经是 Result 并不再重复包装。
     */
    public Result<UserVO> manualWrap(Long id) {
        UserVO vo = getById(id);
        return ResultFactory.success("手动包装示例", vo);
    }

    /**
     * 实体转 VO：去掉 password 等敏感字段。
     */
    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .name(user.getName())
                .age(user.getAge())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }
}
