package com.example.ee.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.example.ee.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * 订单导出视图对象。
 *
 * <p>使用 {@link ExcelProperty} 指定表头，{@link ColumnWidth} 控制列宽。
 * 注意：不要在这里写复杂样式，样式对象非常占内存。</p>
 */
@Data
public class OrderExportVO {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExcelProperty("订单编号")
    @ColumnWidth(20)
    private String orderNo;

    @ExcelProperty("用户ID")
    @ColumnWidth(12)
    private Long userId;

    @ExcelProperty("用户名")
    @ColumnWidth(16)
    private String username;

    @ExcelProperty("商品名称")
    @ColumnWidth(30)
    private String productName;

    @ExcelProperty("订单金额")
    @ColumnWidth(14)
    private BigDecimal amount;

    @ExcelProperty("订单状态")
    @ColumnWidth(12)
    private String status;

    @ExcelProperty("下单时间")
    @ColumnWidth(20)
    private String orderTime;

    @ExcelProperty("收货地址")
    @ColumnWidth(40)
    private String address;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;

    /**
     * 从 Order 实体构造导出 VO。
     */
    public static OrderExportVO from(Order order) {
        OrderExportVO vo = new OrderExportVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setUsername(order.getUsername());
        vo.setProductName(order.getProductName());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setOrderTime(order.getOrderTime().format(DTF));
        vo.setAddress(order.getAddress());
        vo.setRemark(order.getRemark());
        return vo;
    }
}
