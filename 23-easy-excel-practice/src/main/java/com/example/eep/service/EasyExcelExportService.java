package com.example.eep.service;

import com.alibaba.excel.EasyExcel;
import com.example.eep.entity.Product;
import com.example.eep.excel.easyexcel.ProductExportVO;
import com.example.eep.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EasyExcel 导出服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EasyExcelExportService {

    private final ProductRepository productRepository;

    /**
     * 初始化模拟商品数据。
     */
    public Map<String, Object> initProducts() {
        productRepository.deleteAll();
        String[] types = {"国内机票", "国际机票", "酒店", "接送机", "贵宾厅"};
        List<Product> list = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Product p = new Product();
            p.setTypeName(types[i % types.length]);
            p.setNeedPay(i % 2);
            p.setPrice(BigDecimal.valueOf(100 + i + 0.99));
            p.setIsDefault(i % 3 == 0 ? 1 : 0);
            p.setLoungeCode("LC" + String.format("%04d", i));
            list.add(p);
        }
        productRepository.saveAll(list);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", list.size());
        map.put("tip", "商品数据已初始化，可直接导出");
        return map;
    }

    /**
     * 使用 EasyExcel 导出商品，展示自定义 Converter。
     */
    public void exportProducts(HttpServletResponse response) throws IOException {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            throw new IllegalStateException("没有商品数据，请先调用 /api/excel/easyexcel/init 初始化");
        }

        setExcelResponse(response, "products-easyexcel.xlsx");
        List<ProductExportVO> rows = products.stream().map(this::toVO).collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream(), ProductExportVO.class)
                .sheet("商品")
                .doWrite(rows);
    }

    private ProductExportVO toVO(Product product) {
        ProductExportVO vo = new ProductExportVO();
        vo.setTypeName(product.getTypeName());
        vo.setNeedPay(product.getNeedPay());
        vo.setPrice(product.getPrice());
        vo.setIsDefault(product.getIsDefault());
        vo.setLoungeCode(product.getLoungeCode());
        return vo;
    }

    private void setExcelResponse(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encoded);
    }
}
