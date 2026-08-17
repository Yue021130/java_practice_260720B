package com.example.os.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Excel 导入行实体：演示 Optional + Stream 做逐行校验与错误聚合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRow {

    private Integer rowNum;
    private String name;
    private String age;
    private String email;
    private String phone;
    private String amount;

    /**
     * 清洗后的业务对象：校验通过时填充。
     */
    private ImportedOrder converted;

    /**
     * 校验失败原因：校验不通过时填充。
     */
    private String errorMsg;

    /**
     * 导入成功后对应的业务对象。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportedOrder {
        private String name;
        private Integer age;
        private String email;
        private String phone;
        private BigDecimal amount;
    }
}
