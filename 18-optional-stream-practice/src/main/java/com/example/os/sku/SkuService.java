package com.example.os.sku;

import com.example.os.domain.Product;
import com.example.os.domain.Sku;
import com.example.os.support.MockDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 05 SKU 最优价格：演示 Optional.orElseGet 惰性默认值 + Stream.min/max 取极值。
 *
 * <p>真实场景：电商商品详情页展示“到手价”、“最低价 SKU”、“有货 SKU”等。
 * 商品可能不存在、可能没有 SKU、SKU 价格可能为空，Optional + Stream 能优雅处理这些分支。</p>
 */
@Service
@RequiredArgsConstructor
public class SkuService {

    private final MockDataRepository repository;

    /**
     * 查询指定商品的最优 SKU 价格信息。
     *
     * @param productId 商品 ID
     * @return 价格分析结果
     */
    public Map<String, Object> bestPrice(Long productId) {
        // 1. Optional 解包商品：不存在时直接返回降级信息。
        return repository.findProductById(productId)
                .map(this::analyzeSkus)
                .orElseGet(() -> {
                    Map<String, Object> fail = new LinkedHashMap<>();
                    fail.put("productId", productId);
                    fail.put("found", false);
                    fail.put("reason", "商品不存在");
                    fail.put("interviewNote", "orElseGet 是惰性求值：商品不存在时不会执行 analyzeSkus 里的 Stream 计算。");
                    return fail;
                });
    }

    /**
     * 分析商品的 SKU 价格。
     */
    private Map<String, Object> analyzeSkus(Product product) {
        // 2. 过滤有效 SKU：启用、有库存、价格不为空。
        List<Sku> validSkus = repository.getSkus().stream()
                .filter(s -> s.getProductId().equals(product.getId()))
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
                .filter(s -> s.getStock() != null && s.getStock() > 0)
                .filter(s -> s.getPrice() != null)
                .collect(Collectors.toList());

        // 3. Optional 解包最低价 / 最高价：没有有效 SKU 时给默认值。
        BigDecimal minPrice = validSkus.stream()
                .map(Sku::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = validSkus.stream()
                .map(Sku::getPrice)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        // 4. 找到最低价的那个 SKU 详情。
        Map<String, Object> bestSku = validSkus.stream()
                .min(Comparator.comparing(Sku::getPrice))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("skuCode", s.getSkuCode());
                    m.put("skuName", s.getSkuName());
                    m.put("price", s.getPrice());
                    m.put("stock", s.getStock());
                    return m;
                })
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", product.getId());
        result.put("productName", product.getName());
        result.put("category", product.getCategory());
        result.put("found", true);
        result.put("validSkuCount", validSkus.size());
        result.put("minPrice", minPrice);
        result.put("maxPrice", maxPrice);
        result.put("bestSku", bestSku);
        result.put("interviewNote", "Stream.min/max 返回 Optional，再用 orElse 给默认值；Comparator.comparing 可直接比较 BigDecimal。");
        return result;
    }

    /**
     * 八股速记。
     */
    public Map<String, Object> explain() {
        Map<String, Object> result = new HashMap<>();
        result.put("scenario", "SKU 最优价格");
        result.put("pattern", "Optional 解包商品 → Stream 过滤有效 SKU → min/max 取极值 → orElse 兜底");
        result.put("keyPoints", new String[]{
                "orElseGet 适合默认值需要计算的场景，orElse 会立即求值",
                "Stream.min/max 配合 Comparator.naturalOrder() 或 Comparator.comparing()",
                "过滤条件多时用多个 filter，比写一个大 Predicate 可读性更好",
                "Boolean.TRUE.equals(flag) 能安全处理 Boolean 可能为 null 的情况"
        });
        result.put("trap", "直接用 sku.getPrice() 做 min 时如果 price 为 null 会 NPE；一定要先 filter(price != null) 或用 Comparator.nullsLast。");
        return result;
    }
}
