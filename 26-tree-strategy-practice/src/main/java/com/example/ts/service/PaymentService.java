package com.example.ts.service;

import com.example.ts.common.BusinessException;
import com.example.ts.dto.PayRequestDTO;
import com.example.ts.strategy.payment.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付服务
 *
 * <p>策略模式实战：把所有 PaymentStrategy 实现注入到 Map 中，
 * key 为 getType() 返回值，运行时根据请求参数动态选择策略。</p>
 */
@Slf4j
@Service
public class PaymentService {

    private final List<PaymentStrategy> strategies;
    private Map<String, PaymentStrategy> strategyMap;

    public PaymentService(List<PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getType, s -> s));
        log.info("已加载支付策略：{}", strategyMap.keySet());
    }

    /**
     * 执行支付
     */
    public BigDecimal pay(PayRequestDTO request) {
        PaymentStrategy strategy = strategyMap.get(request.getPayType());
        if (strategy == null) {
            throw new BusinessException("不支持的支付方式：" + request.getPayType());
        }
        return strategy.pay(request);
    }
}
