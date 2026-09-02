package com.example.bd.service;

import com.example.bd.dto.CalcRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 高精度金额计算服务。
 *
 * <p>八股要点：</p>
 * <ul>
 *     <li>禁止使用 double 构造 BigDecimal，应使用 String 或 valueOf。</li>
 *     <li>比较大小使用 compareTo，equals 会比较 scale。</li>
 *     <li>除法必须指定 scale 和 RoundingMode，否则可能抛 ArithmeticException。</li>
 *     <li>设置 scale 使用 setScale(scale, roundingMode)。</li>
 * </ul>
 */
@Service
public class AmountCalculator {

    /**
     * 订单金额计算：总价 = 单价 * 数量 * 折扣 + 税费。
     *
     * <p>taxRate 为百分比数值，例如 6 表示 6%。</p>
     */
    public Map<String, Object> calculateOrderAmount(CalcRequest req) {
        int scale = req.getScale();
        RoundingMode mode = RoundingMode.HALF_UP;

        // 1. 商品原价
        BigDecimal original = req.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));

        // 2. 折扣后金额
        BigDecimal discount = req.getDiscount() == null ? BigDecimal.ONE : req.getDiscount();
        BigDecimal discounted = original.multiply(discount).setScale(scale, mode);

        // 3. 税费
        BigDecimal taxRate = req.getTaxRate() == null ? BigDecimal.ZERO : req.getTaxRate();
        BigDecimal tax = discounted.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), scale, mode);

        // 4. 应付金额
        BigDecimal total = discounted.add(tax).setScale(scale, mode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("price", req.getPrice().toPlainString());
        result.put("quantity", req.getQuantity());
        result.put("originalAmount", original.setScale(scale, mode).toPlainString());
        result.put("discount", discount.toPlainString());
        result.put("discountedAmount", discounted.toPlainString());
        result.put("taxRate", taxRate.toPlainString() + "%");
        result.put("taxAmount", tax.toPlainString());
        result.put("totalAmount", total.toPlainString());
        return result;
    }

    /**
     * 分账计算：把 total 按比例分给多方，最后一方拿剩余金额避免精度丢失。
     *
     * <p>校验 platformRate + merchantRate <= 1，防止剩余金额为负。</p>
     */
    public Map<String, Object> splitAmount(BigDecimal total, BigDecimal platformRate, BigDecimal merchantRate, int scale) {
        if (platformRate == null || merchantRate == null) {
            throw new IllegalArgumentException("分账比例不能为空");
        }
        if (platformRate.compareTo(BigDecimal.ZERO) < 0 || merchantRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("分账比例不能为负数");
        }
        BigDecimal sumRate = platformRate.add(merchantRate);
        if (sumRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("分账比例之和不能超过 1");
        }

        RoundingMode mode = RoundingMode.HALF_UP;

        BigDecimal platform = total.multiply(platformRate).setScale(scale, mode);
        BigDecimal merchant = total.multiply(merchantRate).setScale(scale, mode);
        BigDecimal remaining = total.subtract(platform).subtract(merchant).setScale(scale, mode);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total.setScale(scale, mode).toPlainString());
        result.put("platformRate", platformRate.toPlainString());
        result.put("merchantRate", merchantRate.toPlainString());
        result.put("platformAmount", platform.toPlainString());
        result.put("merchantAmount", merchant.toPlainString());
        result.put("remainingAmount", remaining.toPlainString());
        result.put("sumCheck", platform.add(merchant).add(remaining).toPlainString());
        return result;
    }

    /**
     * BigDecimal 常见坑演示。
     */
    public Map<String, Object> pitfalls() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 坑 1：double 构造产生精度问题
        BigDecimal fromDouble = new BigDecimal(0.1);
        BigDecimal fromString = new BigDecimal("0.1");
        BigDecimal fromValueOf = BigDecimal.valueOf(0.1);

        result.put("new BigDecimal(0.1)", fromDouble.toPlainString());
        result.put("new BigDecimal(\"0.1\")", fromString.toPlainString());
        result.put("BigDecimal.valueOf(0.1)", fromValueOf.toPlainString());

        // 坑 2：equals 会比较 scale
        BigDecimal a = new BigDecimal("1.0");
        BigDecimal b = new BigDecimal("1.00");
        result.put("1.0 equals 1.00", a.equals(b));
        result.put("1.0 compareTo 1.00", a.compareTo(b));

        // 坑 3：除法必须指定 scale
        try {
            BigDecimal divide = new BigDecimal("10").divide(new BigDecimal("3"));
            result.put("10/3 不指定 scale", divide.toPlainString());
        } catch (ArithmeticException e) {
            result.put("10/3 不指定 scale", "抛异常：" + e.getMessage());
        }
        result.put("10/3 HALF_UP scale=2",
                new BigDecimal("10").divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP).toPlainString());

        return result;
    }
}
