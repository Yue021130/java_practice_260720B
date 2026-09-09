package com.example.ts.service;

import com.example.ts.common.BusinessException;
import com.example.ts.dto.DiscountRequestDTO;
import com.example.ts.strategy.discount.DiscountStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 折扣服务
 *
 * <p>策略模式实战：不同用户身份/活动使用不同折扣算法，
 * 新增折扣策略只需新增实现类，无需修改既有代码（开闭原则）。</p>
 */
@Slf4j
@Service
public class DiscountService {

    private final List<DiscountStrategy> strategies;
    private Map<String, DiscountStrategy> strategyMap;

    public DiscountService(List<DiscountStrategy> strategies) {
        this.strategies = strategies;
    }

    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(DiscountStrategy::getType, s -> s));
        log.info("已加载折扣策略：{}", strategyMap.keySet());
    }

    /**
     * 计算折扣后金额
     */
    public BigDecimal calculate(DiscountRequestDTO request) {
        DiscountStrategy strategy = strategyMap.get(request.getStrategyType());
        if (strategy == null) {
            throw new BusinessException("不支持的折扣策略：" + request.getStrategyType());
        }
        return strategy.calculate(request);
    }
}
