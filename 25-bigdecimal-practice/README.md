# 25. 高精度金额计算实战（BigDecimal）

## 项目定位

Spring Boot 2.7 + BigDecimal 实战，覆盖：

- 订单金额高精度计算（单价 × 数量 × 折扣 + 税费）
- 分账计算（最后一方拿剩余避免精度丢失）
- BigDecimal 常见坑：double 构造、equals vs compareTo、除法异常
- Vue3 前端计算器 + 八股速记

> 真实业务场景：电商订单结算、优惠券抵扣、税费计算、平台分账。

---

## 技术栈

| 技术 | 版本 | 作用 |
|------|------|------|
| Spring Boot | 2.7.18 | Web 容器 |
| Vue3 + Vite | 3.4 + 5.2 | 前端演示 |

---

## 快速启动

### 后端

```bash
cd 25-bigdecimal-practice
mvn spring-boot:run
```

服务端口：`8105`

### 前端

```bash
cd 25-bigdecimal-practice/web
npm install
npm run dev
```

前端端口：`5198`，代理到后端 `8105`。

---

## 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/amount/calculate` | 订单金额计算（taxRate 为百分比数值，如 6 表示 6%） |
| GET | `/api/amount/split` | 分账计算（platformRate + merchantRate <= 1） |
| GET | `/api/amount/pitfalls` | 常见坑演示 |
| GET | `/api/amount/explain` | 八股速记 |

---

## 核心代码解读

### 1. 禁止使用 double 构造

```java
// 错误：0.1000000000000000055511151231257827021181583404541015625
new BigDecimal(0.1);

// 正确
new BigDecimal("0.1");
BigDecimal.valueOf(0.1); // 内部转 String
```

### 2. 除法必须指定 scale

```java
BigDecimal result = a.divide(b, 2, RoundingMode.HALF_UP);
```

### 3. 比较大小用 compareTo

```java
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");
a.equals(b);      // false，scale 不同
a.compareTo(b);   // 0，数值相等
```

### 4. 分账比例校验

```java
if (platformRate.add(merchantRate).compareTo(BigDecimal.ONE) > 0) {
    throw new IllegalArgumentException("分账比例之和不能超过 1");
}
```

---

## 八股速记

1. **为什么金额不能用 double？**
   - double 是二进制浮点，无法精确表示十进制小数。

2. **BigDecimal 构造推荐哪种？**
   - `BigDecimal.valueOf(double)` 或 `new BigDecimal(String)`。

3. **除法抛 ArithmeticException 怎么办？**
   - 使用 `divide(divisor, scale, RoundingMode)`。

4. **equals 和 compareTo 区别？**
   - `equals` 比较值和 scale；`compareTo` 只比较数值大小。

5. **怎么保留两位小数？**
   - `setScale(2, RoundingMode.HALF_UP)`。

6. **taxRate 传什么格式？**
   - 传百分比数值，例如 `6` 表示 6%，后端会自动除以 100。

7. **分账时如何保证剩余金额非负？**
   - 校验 platformRate + merchantRate <= 1，最后一方拿剩余金额。

---

## 测试

```bash
mvn test
```

- `AmountCalculatorTest`：订单计算、分账、常见坑、除不尽、比例越界
- `AmountControllerTest`：接口测试、参数校验 400、分账越界 400

---

## 作者

Yue021130
