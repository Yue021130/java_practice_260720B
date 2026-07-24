# Java 场景模拟测试

始于 2026 年 7 月 20 日。

本仓库是「java高级知识」系列的可运行场景练习集，每个专题都用 **Spring Boot 后端 + 前端面板** 把知识点包装成可交互的实验场景，
配合中文注释与 README 中的「面试八股」速记，边跑边学。

## 已完成的专题

| 目录 | 主题 | 前端 | 后端端口 | 前端端口 |
| --- | --- | --- | --- | --- |
| [01-thread-pool-practice](01-thread-pool-practice/) | Java 线程池实践：七大参数、四种拒绝策略、动态调参 | React 18 + Vite | 8081 | 5174 |
| [02-juc-practice](02-juc-practice/) | Java 并发包（JUC）全场景：锁 / 原子类 / 并发容器 / 同步工具 / CompletableFuture / ThreadLocal / JMM | Vue 3 + Vite | 8082 | 5175 |
| [03-collections-generics-practice](03-collections-generics-practice/) | Java 集合与泛型全场景：List / Set / Map / Queue / 工具类 / 泛型 / 综合实战 | Vue 3 + Vite | 8083 | 5176 |

## 通用启动方式

进入对应子目录后：

```bash
# 后端
mvn spring-boot:run

# 前端（另开终端）
cd web
npm install
npm run dev
```

然后按上表端口打开浏览器即可。

## 统一技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + springdoc-openapi 1.7.0
- 前端：Vite 5 + axios + 纯手写 CSS（无 UI 库）
- 测试：JUnit 5 + MockMvc + AssertJ

每个子目录都有独立的 `README.md`，详细说明该专题的场景清单、面试要点、接口文档与实验顺序。
