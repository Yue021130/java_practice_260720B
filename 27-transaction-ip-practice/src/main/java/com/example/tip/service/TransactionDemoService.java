package com.example.tip.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.tip.dto.TransactionSceneVO;
import com.example.tip.entity.Account;
import com.example.tip.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 事务失效场景演示服务
 *
 * <p>八股：@Transactional 失效的 8 大场景
 * 1. 同类自调用（this.method）导致事务代理失效；
 * 2. 方法不是 public；
 * 3. 异常被 try-catch 吞掉；
 * 4. 异常类型不匹配 rollbackFor；
 * 5. 在 @Async 异步方法中回滚不会传播到调用方；
 * 6. 数据库引擎不支持事务（如 MyISAM）；
 * 7. 传播行为配置错误；
 * 8. 类未被 Spring 管理（未加 @Service 等）。
 * </p>
 */
@Slf4j
@Service
public class TransactionDemoService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    @Lazy
    private TransactionDemoService self;

    /**
     * 重置账户余额，便于重复演示
     */
    public void reset() {
        Account account = new Account();
        account.setBalance(new BigDecimal("1000.00"));
        accountMapper.update(account, new LambdaUpdateWrapper<Account>()
                .ne(Account::getId, -1L));
    }

    public BigDecimal getBalance(Long id) {
        Account account = accountMapper.selectById(id);
        return account == null ? BigDecimal.ZERO : account.getBalance();
    }

    /**
     * 场景1：同类自调用导致 @Transactional 失效
     */
    public TransactionSceneVO scene1SelfInvoke() {
        reset();
        try {
            // 这里用 this 调用，事务不生效
            this.transferWithTx(1L, 2L, new BigDecimal("100.00"));
        } catch (Exception e) {
            log.info("场景1捕获异常：{}", e.getMessage());
        }
        boolean rollbackSuccess = new BigDecimal("1000.00").equals(getBalance(1L))
                && new BigDecimal("1000.00").equals(getBalance(2L));
        return new TransactionSceneVO(
                "同类自调用",
                "通过 this 调用 @Transactional 方法，事务代理未生效，异常后数据未回滚",
                rollbackSuccess,
                rollbackSuccess ? "数据一致，事务生效" : "数据不一致，事务失效"
        );
    }

    /**
     * 场景1 修复版：通过注入的代理对象调用
     */
    public TransactionSceneVO scene1Fix() {
        reset();
        try {
            // 通过 @Lazy 注入的代理对象调用，事务生效
            self.transferWithTx(1L, 2L, new BigDecimal("100.00"));
        } catch (Exception e) {
            log.info("场景1修复版捕获异常：{}", e.getMessage());
        }
        boolean rollbackSuccess = new BigDecimal("1000.00").equals(getBalance(1L))
                && new BigDecimal("1000.00").equals(getBalance(2L));
        return new TransactionSceneVO(
                "同类自调用-修复",
                "通过 @Lazy 注入的代理对象调用，事务生效，异常后回滚",
                rollbackSuccess,
                rollbackSuccess ? "数据一致，事务生效" : "数据不一致，事务失效"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferWithTx(Long fromId, Long toId, BigDecimal amount) throws Exception {
        doTransfer(fromId, toId, amount);
        throw new Exception("模拟业务异常，触发回滚");
    }

    private void doTransfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountMapper.selectById(fromId);
        Account to = accountMapper.selectById(toId);
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountMapper.updateById(from);
        accountMapper.updateById(to);
    }

    /**
     * 场景2：异常被吞掉导致事务不生效
     */
    public TransactionSceneVO scene2SwallowException() {
        reset();
        swallowExceptionTransfer(1L, 2L, new BigDecimal("100.00"));
        boolean rollbackSuccess = new BigDecimal("1000.00").equals(getBalance(1L))
                && new BigDecimal("1000.00").equals(getBalance(2L));
        return new TransactionSceneVO(
                "异常被吞掉",
                "方法内部 try-catch 吞掉异常，Spring 无法感知，事务不会回滚",
                rollbackSuccess,
                rollbackSuccess ? "数据一致，事务生效" : "数据不一致，事务失效"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void swallowExceptionTransfer(Long fromId, Long toId, BigDecimal amount) {
        try {
            doTransfer(fromId, toId, amount);
            int i = 1 / 0;
        } catch (Exception e) {
            log.info("异常被吞掉：{}", e.getMessage());
        }
    }

    /**
     * 场景3：rollbackFor 配置错误（默认不回滚受检异常）
     */
    public TransactionSceneVO scene3WrongRollbackFor() {
        reset();
        try {
            // @Transactional 默认 rollbackFor = RuntimeException，受检异常不会回滚
            checkedExceptionTransfer(1L, 2L, new BigDecimal("100.00"));
        } catch (Exception e) {
            log.info("场景3捕获异常：{}", e.getMessage());
        }
        boolean rollbackSuccess = new BigDecimal("1000.00").equals(getBalance(1L))
                && new BigDecimal("1000.00").equals(getBalance(2L));
        return new TransactionSceneVO(
                "rollbackFor 不匹配",
                "抛出受检异常 Exception，但 @Transactional 默认只回滚 RuntimeException",
                rollbackSuccess,
                rollbackSuccess ? "数据一致，事务生效" : "数据不一致，事务失效"
        );
    }

    @Transactional
    public void checkedExceptionTransfer(Long fromId, Long toId, BigDecimal amount) throws Exception {
        doTransfer(fromId, toId, amount);
        throw new Exception("受检异常");
    }

    /**
     * 场景4：非 public 方法导致事务失效
     */
    public TransactionSceneVO scene4NonPublic() {
        reset();
        try {
            nonPublicTransfer(1L, 2L, new BigDecimal("100.00"));
        } catch (Exception e) {
            log.info("场景4捕获异常：{}", e.getMessage());
        }
        boolean rollbackSuccess = new BigDecimal("1000.00").equals(getBalance(1L))
                && new BigDecimal("1000.00").equals(getBalance(2L));
        return new TransactionSceneVO(
                "非 public 方法",
                "@Transactional 只能作用于 public 方法，private/protected 不生效",
                rollbackSuccess,
                rollbackSuccess ? "数据一致，事务生效" : "数据不一致，事务失效"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    private void nonPublicTransfer(Long fromId, Long toId, BigDecimal amount) throws Exception {
        doTransfer(fromId, toId, amount);
        throw new Exception("异常");
    }

    /**
     * 场景5：@Async 异步方法中事务独立
     *
     * <p>注意：同一个方法上同时加 @Async 和 @Transactional 会导致事务代理失效，
     * 因此这里让 @Async 方法通过代理对象调用 @Transactional 方法。</p>
     */
    public TransactionSceneVO scene5Async() throws Exception {
        reset();
        try {
            asyncTransfer(1L, 2L, new BigDecimal("100.00"));
        } catch (Exception e) {
            log.info("场景5捕获异常：{}", e.getMessage());
        }
        // 等待异步执行完成
        Thread.sleep(500);
        boolean rollbackSuccess = new BigDecimal("1000.00").equals(getBalance(1L))
                && new BigDecimal("1000.00").equals(getBalance(2L));
        return new TransactionSceneVO(
                "@Async 异步事务",
                "异步方法在独立线程中执行事务，调用方无法同步感知其回滚，需配合返回值/回调",
                rollbackSuccess,
                rollbackSuccess ? "异步事务自身回滚成功" : "异步事务自身未回滚"
        );
    }

    @Async
    public void asyncTransfer(Long fromId, Long toId, BigDecimal amount) throws Exception {
        // 通过代理对象调用，确保 @Transactional 生效
        self.asyncTransferWithTx(fromId, toId, amount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void asyncTransferWithTx(Long fromId, Long toId, BigDecimal amount) throws Exception {
        doTransfer(fromId, toId, amount);
        throw new Exception("异步异常");
    }
}
