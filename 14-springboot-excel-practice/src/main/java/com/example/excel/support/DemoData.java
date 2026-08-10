package com.example.excel.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 演示数据工厂。
 *
 * 提供各模块共享的「用户」「订单」演示数据：
 * - users()：固定 8 条用户，供快速开始 / 注解 / 样式 / 监听器使用
 * - user(i)：按序号确定性生成一条用户，供大数据量导出逐行生成
 * - orders()：固定 8 条订单，供复杂表头 / 模板填充使用
 *
 * 刻意把数据源抽到这里，模拟「数据库查出来的实体」，模块里再映射成各自的 head 类，
 * 贴近真实工程结构。
 */
public final class DemoData {

    private static final String[] DEPARTMENTS = {"研发部", "产品部", "市场部", "运营部", "人事部", "财务部"};
    private static final String[] NAMES = {
            "张伟", "李娜", "王强", "赵敏", "刘洋", "陈静", "杨磊", "黄丽",
            "周涛", "吴倩", "徐鹏", "孙悦", "胡军", "朱琳", "高峰", "马超"
    };
    private static final String[] CUSTOMERS = {
            "北京云启科技", "上海蓝湾商贸", "广州星辰互联", "深圳万象数据",
            "杭州西子电商", "成都锦程物流", "武汉江城软件", "西安秦岭实业"
    };
    private static final String[] STATUS = {"已支付", "待支付", "已取消"};

    private DemoData() {
    }

    /**
     * 固定 8 条用户演示数据。
     */
    public static List<UserRow> users() {
        List<UserRow> list = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            list.add(user(i));
        }
        return list;
    }

    /**
     * 按序号确定性生成一条用户（id、姓名、部门、薪资、入职日期、在职状态）。
     *
     * 相同 i 永远得到相同数据，方便测试断言与大数据量演示。
     */
    public static UserRow user(int i) {
        Calendar cal = Calendar.getInstance();
        cal.set(2018 + (i % 6), (i * 3) % 12, (i * 7) % 28 + 1, 9, 30, 0);
        return new UserRow(
                i,
                NAMES[(i - 1) % NAMES.length],
                DEPARTMENTS[(i - 1) % DEPARTMENTS.length],
                8000.0 + (i * 1377.5),
                cal.getTime(),
                i % 7 != 0);
    }

    /**
     * 固定 8 条订单演示数据。
     */
    public static List<OrderRow> orders() {
        List<OrderRow> list = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Calendar cal = Calendar.getInstance();
            cal.set(2025, (i * 2) % 12, (i * 5) % 28 + 1, 14, 20, 0);
            list.add(new OrderRow(
                    "SO2025" + String.format("%04d", i),
                    CUSTOMERS[(i - 1) % CUSTOMERS.length],
                    new BigDecimal("1250.00").multiply(BigDecimal.valueOf(i)),
                    STATUS[(i - 1) % STATUS.length],
                    cal.getTime()));
        }
        return list;
    }

    /**
     * 直接拿到系统当前时间（供比较展示）。
     */
    public static Date now() {
        return new Date();
    }
}
