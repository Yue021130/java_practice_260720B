package com.example.scp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.scp.cache.RedisLockClient;
import com.example.scp.cache.TwoLevelCache;
import com.example.scp.cache.TwoLevelCacheManager;
import com.example.scp.common.BusinessException;
import com.example.scp.dto.ProductSaveDTO;
import com.example.scp.dto.ProductVO;
import com.example.scp.entity.Product;
import com.example.scp.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 商品 Service：本章教学重点——Spring Cache 注解 + 二级缓存的三个经典问题防护落点。
 *
 * <p>缓存设计总览：</p>
 * <ul>
 *   <li>{@code product}：详情缓存，key = 商品 id。查不到的 id 以 NullValue
 *       占位符进缓存（30s 短 TTL）——「缓存穿透」防护；</li>
 *   <li>{@code productPage}：分页列表缓存，key 由 productPageKeyGenerator 按
 *       "page:current:size:name:category:status" 生成，新增/修改/删除时
 *       allEntries 整体失效；</li>
 *   <li>{@code hotDetail()}：热点详情击穿演示，不走注解，手写「缓存 +
 *       Redis 分布式锁 + 双重检查」——「缓存击穿」防护；</li>
 *   <li>TTL 带随机抖动（TwoLevelCache.put 内部完成）——「缓存雪崩」防护。</li>
 * </ul>
 */
@Slf4j
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final TwoLevelCacheManager cacheManager;
    private final RedisLockClient redisLockClient;

    public ProductService(ProductMapper productMapper,
                          TwoLevelCacheManager cacheManager,
                          RedisLockClient redisLockClient) {
        this.productMapper = productMapper;
        this.cacheManager = cacheManager;
        this.redisLockClient = redisLockClient;
    }

    /**
     * 分页查询。
     *
     * <p>缓存设计意图：</p>
     * <ul>
     *   <li>value="productPage"：分页结果单独一个缓存区，与详情缓存隔离，
     *       互不影响失效策略；</li>
     *   <li>keyGenerator="productPageKeyGenerator"：分页参数是复合条件
     *       （current/size/name/category/status），默认 SimpleKeyGenerator
     *       生成的 key 可读性差，自定义生成 "page:1:10:手机:-:-"
     *       这样的可读 key，方便 Redis 里直接观察；</li>
     *   <li>condition="#size <= 20"：分页结果变化频繁，只适合缓存翻页深度
     *       浅的小页；若有人 size=10000 拉全表，缓存下来既占内存又极易失效，
     *       大页直接放行不缓存。</li>
     * </ul>
     *
     * <p>对应缓存问题：列表页变化快、key 组合多，是雪崩与内存浪费的重灾区，
     * 用 condition 控制准入 + 写时带随机抖动兜底。</p>
     */
    @Cacheable(value = "productPage", keyGenerator = "productPageKeyGenerator", condition = "#size <= 20")
    public IPage<ProductVO> page(long current, long size, String name, String category, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .and(StringUtils.hasText(name),
                        w -> w.like(Product::getName, name).or().like(Product::getCategory, name))
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .eq(status != null, Product::getStatus, status)
                .orderByAsc(Product::getSortOrder)
                .orderByDesc(Product::getId);
        Page<Product> entityPage = productMapper.selectPage(new Page<>(current, size), wrapper);
        // records 从 Product 换成 ProductVO 后放回，分页元数据（total/pages）保持不动
        Page<ProductVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(this::toVO)
                .collect(java.util.stream.Collectors.toList()));
        return voPage;
    }

    /**
     * 商品详情：标准 @Cacheable 用法。
     *
     * <p>缓存设计意图：</p>
     * <ul>
     *   <li>value="product"：详情缓存区；key="#id"：业务 key 就是商品 id，
     *       Redis 中体现为 "product::1"；</li>
     *   <li><b>故意不写 unless="#result == null"</b>：Spring Cache 默认
     *       result 为 null 时不缓存，查不到的商品会每次穿透到数据库。
     *       TwoLevelCache.put 内部把 null 转成 NullValue 占位符写入，
     *       并给占位符配 30s 短 TTL——恶意用不存在的 id 反复请求时，
     *       请求被占位符挡在缓存层，数据库只被打一次。</li>
     * </ul>
     *
     * <p>这正是「缓存穿透」防护的落点：缓存空值 + 短 TTL 兜底。</p>
     */
    @Cacheable(value = "product", key = "#id")
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        return product == null ? null : toVO(product);
    }

    /**
     * 热点商品详情：缓存击穿演示接口，故意不使用任何缓存注解。
     *
     * <p>「缓存击穿」场景：热点 key 在过期瞬间，大量并发同时 miss，
     * 请求一齐打到数据库。防护思路：只允许一个线程回源重建，
     * 其余线程等待或降级。手动实现一遍才能看懂注解替你做了什么：</p>
     * <ol>
     *   <li>先查二级缓存（L1+L2）命中直接返回；</li>
     *   <li>未命中则走 {@link RedisLockClient#loadWithLock}：
     *       SETNX 互斥锁只允许一个线程进回源逻辑；</li>
     *   <li>拿到锁的线程「双重检查」——再查一次缓存（等待期间可能
     *       已被别人重建），仍 miss 才查库；查到的值 put 回缓存供
     *       后续请求与自旋线程读取；</li>
     *   <li>没拿到锁的线程自旋等待持锁者写完缓存后直接读缓存。</li>
     * </ol>
     */
    public ProductVO hotDetail(Long id) {
        TwoLevelCache cache = cacheManager.getTwoLevelCache("product");
        ProductVO cached = cache.get(id, ProductVO.class);
        if (cached != null) {
            return cached;
        }
        return redisLockClient.loadWithLock(String.valueOf(id), () -> {
            // 双重检查：拿到锁不代表一定要查库，先再看一次缓存
            ProductVO again = cache.get(id, ProductVO.class);
            if (again != null) {
                return again;
            }
            Product product = productMapper.selectById(id);
            if (product == null) {
                return null;
            }
            ProductVO vo = toVO(product);
            cache.put(id, vo);
            return vo;
        }, 10);
    }

    /**
     * 新增商品。
     *
     * <p>缓存设计意图：新增一条数据后，所有命中过旧数据的分页结果都不再
     * 可信（total 变了、页内数据变了），而分页 key 是复合参数拼出来的，
     * 无法精确反推哪些 key 受影响，只能整区失效——allEntries=true 清空
     * 整个 productPage 缓存。详情缓存 key 是商品 id，新增的是新 id，
     * 不涉及，不用清。</p>
     */
    @CacheEvict(value = "productPage", allEntries = true)
    public ProductVO save(ProductSaveDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getSortOrder() == null) {
            product.setSortOrder(0);
        }
        productMapper.insert(product);
        log.info("新增商品 [{}]，分页缓存已整体失效", product.getId());
        return toVO(product);
    }

    /**
     * 修改商品。
     *
     * <p>缓存设计意图（@Caching 组合两个缓存操作）：</p>
     * <ul>
     *   <li>@CachePut(value="product", key="#dto.id")：方法执行<b>之后</b>
     *       把返回值写回详情缓存——与 @Cacheable 不同，@CachePut 不会跳过
     *       方法体，保证数据库和缓存同步更新，下次 getById 直接读到新值，
     *       不会出现「改了但缓存还是旧值」的脏读窗口；</li>
     *   <li>@CacheEvict(value="productPage", allEntries=true)：同新增，
     *       分页结果不可信，整区失效。</li>
     * </ul>
     */
    @Caching(evict = {@CacheEvict(value = "productPage", allEntries = true)})
    @CachePut(value = "product", key = "#dto.id")
    public ProductVO update(ProductSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "修改商品时 id 不能为空");
        }
        Product exist = productMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(404, "商品不存在或已删除");
        }
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        if (product.getStatus() == null) {
            product.setStatus(exist.getStatus());
        }
        if (product.getSortOrder() == null) {
            product.setSortOrder(exist.getSortOrder());
        }
        productMapper.updateById(product);
        log.info("修改商品 [{}]，详情缓存已更新、分页缓存已整体失效", product.getId());
        return toVO(productMapper.selectById(product.getId()));
    }

    /**
     * 删除商品（逻辑删除）。
     *
     * <p>缓存设计意图（@Caching 组合两个失效操作）：</p>
     * <ul>
     *   <li>@CacheEvict(value="product", key="#id")：详情缓存精确删除该
     *       key，后续 getById 会 miss 回源——查库时逻辑删除字段生效
     *       返回 null，再由 NullValue 占位符挡住重复穿透；</li>
     *   <li>@CacheEvict(value="productPage", allEntries=true)：分页结果
     *       不可信，整区失效。</li>
     * </ul>
     */
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "productPage", allEntries = true)
    })
    public void delete(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "商品不存在或已删除");
        }
        productMapper.deleteById(id);
        log.info("删除商品 [{}]，详情与分页缓存已失效", id);
    }

    /** 实体转 VO：字段名一致，BeanUtils 直接拷贝 */
    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
