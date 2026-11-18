package com.example.scp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.scp.common.Result;
import com.example.scp.dto.ProductSaveDTO;
import com.example.scp.dto.ProductVO;
import com.example.scp.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口：分页 / 详情 / 热点详情（击穿演示）/ 新增 / 修改 / 删除。
 *
 * <p>除明确放行外均需登录（Sa-Token 拦截器全局生效），
 * 登录后在 Knife4j 右上角 Authorize 填入 token 即可调试。</p>
 */
@Tag(name = "商品管理", description = "商品 CRUD 与缓存三问演示接口")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "分页查询商品", description = "走 productPage 缓存，size > 20 时不缓存（condition 控制），新增/修改/删除后整区失效")
    @GetMapping("/page")
    public Result<IPage<ProductVO>> page(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "关键字：按名称或分类模糊匹配") @RequestParam(required = false) String name,
            @Parameter(description = "分类精确匹配") @RequestParam(required = false) String category,
            @Parameter(description = "状态：1 上架，0 下架") @RequestParam(required = false) Integer status) {
        return Result.ok(productService.page(current, size, name, category, status));
    }

    @Operation(summary = "商品详情", description = "走 product 缓存；查不到的 id 以空值占位符缓存 30s，防缓存穿透")
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@Parameter(description = "商品 id") @PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @Operation(summary = "热点商品详情", description = "缓存击穿演示接口：手动实现「缓存 + Redis 分布式锁 + 双重检查」，不用 @Cacheable")
    @GetMapping("/hot/{id}")
    public Result<ProductVO> hotDetail(@Parameter(description = "热点商品 id") @PathVariable Long id) {
        return Result.ok(productService.hotDetail(id));
    }

    @Operation(summary = "新增商品", description = "成功后清空整个 productPage 分页缓存")
    @PostMapping("/")
    public Result<ProductVO> save(@Validated @RequestBody ProductSaveDTO dto) {
        return Result.ok(productService.save(dto));
    }

    @Operation(summary = "修改商品", description = "@CachePut 同步更新详情缓存，同时清空整个 productPage 分页缓存")
    @PutMapping("/")
    public Result<ProductVO> update(@Validated @RequestBody ProductSaveDTO dto) {
        return Result.ok(productService.update(dto));
    }

    @Operation(summary = "删除商品", description = "逻辑删除，并失效该商品详情缓存与整个 productPage 分页缓存")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "商品 id") @PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
