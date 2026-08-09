# 13-unsafe-practice：魔法类 Unsafe 实践

本模块把 JDK 的底层"魔法类" `sun.misc.Unsafe` 转化为可运行、可交互的 Spring Boot + Vue 3 项目代码，覆盖**初识与获取、堆外内存、绕过构造器、CAS 原子操作、字段偏移与对象布局、park/unpark、内存屏障、危险与本质**等完整能力，每个实验都在真实的 JVM 上跑给你看，便于系统学习。

**核心价值**：读懂 JUC、Netty、Kafka 等框架"为什么快"的底层原理——`AtomicInteger` 的 CAS、`ConcurrentHashMap` 的扩容、`LockSupport` 的挂起、Netty 的堆外内存，追根到底都是这一个类。

> ⚠️ 本专题定位是**学习与观察**：只做安全可控的实验，真正会崩 JVM 的操作（越界访问）只展示代码形态、不执行。生产代码请远离 `sun.misc.Unsafe`。

## 技术栈

- 后端：Spring Boot 2.7.18 + Java 8 + Maven + Lombok + SpringDoc OpenAPI 1.7.0
- 底层：`sun.misc.Unsafe`（反射获取 `theUnsafe` 单例，JDK 8 / 11 / 17 均可运行）
- 测试：JUnit 5 + MockMvc + AssertJ（含 ABA 现场复现的确定性断言）
- 前端：Vue 3 + Vite 5 + axios + 纯手写 CSS
- 端口：后端 **8093**，前端 **5186**

## 核心：Unsafe 是什么

`sun.misc.Unsafe` 是 JVM 暴露给 JDK 内部使用的"后门"，把 Java 语言层面藏起来的底层能力直接裸露出来，共六大能力：

| 能力 | 关键方法 | 落地场景 |
| --- | --- | --- |
| 内存操作 | `allocateMemory / freeMemory / putInt / getInt / setMemory / copyMemory` | 堆外内存、Netty 缓冲池 |
| 对象实例化 | `allocateInstance(Class)` | Kryo 反序列化、深拷贝 |
| CAS 原子操作 | `compareAndSwapInt / Long / Object` | JUC 原子类、AQS、ConcurrentHashMap |
| 字段偏移 | `objectFieldOffset / arrayBaseOffset / arrayIndexScale` | 打破封装、数组直接寻址 |
| 线程阻塞 | `park / unpark` | LockSupport、AQS 挂起唤醒 |
| 内存屏障 | `loadFence / storeFence / fullFence` | volatile 语义的底层实现 |

**为什么拿不到实例**：`Unsafe.getUnsafe()` 只对 BootstrapClassLoader 加载的类开放（JDK 内部类），普通应用直接调用必抛 `SecurityException`。通行做法是反射读取内部字段 `theUnsafe`（本模块 `UnsafeConfig` 就是这么做的，也是面试题的答案）。

## 模块结构

```
13-unsafe-practice/
├── pom.xml
├── README.md
├── src/main/java/com/example/unsafe/
│   ├── UnsafePracticeApplication.java
│   ├── common/         # 统一响应 ApiResponse、全局异常处理
│   ├── config/         # UnsafeConfig（反射装配 Unsafe Bean）、OpenAPI、CORS
│   ├── intro/          # 01 初识 Unsafe
│   ├── memory/         # 02 堆外内存
│   ├── instance/       # 03 绕过构造器
│   ├── cas/            # 04 CAS 原子操作（自旋/性能对比/ABA）
│   ├── offset/         # 05 字段偏移与对象布局
│   ├── park/           # 06 park/unpark
│   ├── fence/          # 07 内存屏障
│   └── essence/        # 08 危险与本质
├── src/main/resources/application.yml
├── src/test/java/com/example/unsafe/   # 上下文加载测试 + 全场景接口测试
└── web/                # Vue 3 前端面板
```

## 快速启动

### 后端

```bash
cd 13-unsafe-practice
mvn spring-boot:run
```

Swagger UI：http://localhost:8093/swagger-ui/index.html

### 前端

```bash
cd 13-unsafe-practice/web
npm install
npm run dev
```

前端开发服务器：http://localhost:5186

### 运行测试

```bash
cd 13-unsafe-practice
mvn test
```

测试里 `abaDetectsVersionChange` 会真实起两个线程复现 ABA 问题，并断言"无版本号 CAS 成功、带版本号 CAS 失败"的结局。

## 接口速查

### 01. 初识 Unsafe `/api/intro`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/intro/info` | GET | Unsafe 实例、类加载器、六大能力地图 |
| `/api/intro/getunsafe-demo` | GET | 演示 getUnsafe() 被 SecurityException 拒之门外的过程 |
| `/api/intro/why` | GET | 为什么叫"魔法类"、为什么官方禁用 |

### 02. 堆外内存 `/api/memory`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/memory/allocate` | POST | allocateMemory → putInt/getInt 按偏移读写 → freeMemory |
| `/api/memory/setcopy` | GET | setMemory 批量填充 + copyMemory 整体拷贝（十六进制验证） |
| `/api/memory/leak` | POST | 分配 N 块 1MB 堆外内存，观察"堆几乎不变"，演示泄漏风险 |

### 03. 绕过构造器 `/api/instance`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/instance/create` | GET | allocateInstance 不调构造器造对象，对比 new（校验被绕过） |
| `/api/instance/compare` | GET | new vs allocateInstance 对比表 |
| `/api/instance/uses` | GET | Kryo / 深拷贝 / 单例破解 / 反序列化攻击面 |

### 04. CAS 原子操作 `/api/cas`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/cas/spin` | POST | 自旋 CAS 计数器，统计 CAS 尝试次数 |
| `/api/cas/benchmark` | POST | synchronized / AtomicInteger / Unsafe CAS 并发自增耗时对比 |
| `/api/cas/aba` | GET | ABA 问题现场复现（真实两线程：无版本号 vs 带版本号） |
| `/api/cas/explain` | GET | CAS 原理八股速记 |

### 05. 字段偏移与对象布局 `/api/offset`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/offset/fields` | GET | 各字段 objectFieldOffset，推断对象头大小 |
| `/api/offset/directwrite` | GET | 用偏移量绕过 getter/setter 读写 private 字段 |
| `/api/offset/array` | GET | arrayBaseOffset + arrayIndexScale 直接寻址数组 |
| `/api/offset/layout` | GET | 对象内存布局示意图（Mark Word / Klass / 字段 / 填充） |

### 06. park/unpark `/api/park`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/park/demo` | GET | 正常顺序唤醒 + 提前 unpark 许可证机制（完整时间线） |
| `/api/park/compare` | GET | park vs wait/notify 对比表 |
| `/api/park/explain` | GET | LockSupport 原理 / AQS 怎么用 |

### 07. 内存屏障 `/api/fence`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/fence/demo` | GET | 普通字段 + loadFence/storeFence/fullFence 手写 volatile 效果 |
| `/api/fence/explain` | GET | JMM 8 种内存操作 / 4 条 Happens-Before / x86 实现 |

### 08. 危险与本质 `/api/essence`

| 端点 | 方法 | 说明 |
| --- | --- | --- |
| `/api/essence/risks` | GET | 四大风险（越界崩溃 / 堆外泄漏 / 破坏封装 / 不可移植） |
| `/api/essence/essence` | GET | 本质：是什么、为什么存在、绕过了哪些防线 |
| `/api/essence/evolution` | GET | JDK 版本演变 + JEP 193 VarHandle 替代方案 |
| `/api/essence/whouses` | GET | JUC / Netty / Kafka / Cassandra / 序列化框架谁在用 |

## 面试八股

### Unsafe 是什么？为什么叫"魔法类"？

`sun.misc.Unsafe` 是 JDK 内部的一层底层后门，能直接操作内存、绕过构造器造对象、做硬件级原子 CAS、精确控制线程。因为它**绕过了类型安全、封装、内存管理三道 Java 语言防线**，能力像魔法一样，所以叫"魔法类"；也正因如此，官方严禁生产代码直接使用。

### 如何获取 Unsafe 实例？

`Unsafe.getUnsafe()` 会先检查类加载器，只有 BootstrapClassLoader（null）加载的类才能调用，普通类直接调抛 `SecurityException`。标准做法是反射读取内部单例字段：

```java
Field f = Unsafe.class.getDeclaredField("theUnsafe");
f.setAccessible(true);
Unsafe unsafe = (Unsafe) f.get(null);
```

JDK 9+ 中 `sun.misc` 由 `jdk.unsupported` 模块无条件导出，此方法在 JDK 8 / 11 / 17 均可用。

### CAS 是什么？Unsafe 的 compareAndSwap 原理？

CAS = Compare And Swap，一条硬件原子指令（x86 的 `lock cmpxchg`）：比较内存值是否等于期望值，等于才写新值，整体不可打断。`compareAndSwapInt(对象, 字段偏移, 期望值, 新值)` 中偏移量决定对哪个字段动手。JUC 的 `AtomicInteger`、`ConcurrentHashMap`、AQS 全部构建在它之上。

### CAS 与 synchronized 的区别？

锁是"悲观"：先锁再干活，冲突就挂起；CAS 是"乐观"：直接尝试，失败就重试（自旋）。CAS 更轻量，但自旋空转耗 CPU、只能保证一个变量的原子性、有 ABA 问题。

### CAS 的三大问题与 ABA 如何解决？

1. 自旋空转耗 CPU；2. 只能保证一个变量原子；3. **ABA**：线程 A 读到 100，B 改成 200 又改回 100，A 的 CAS(100→50) 成功，但对中间过程一无所知。解决：带版本号/时间戳，如 `AtomicStampedReference`——版本 0→1→2 变了，CAS 就失败。本模块 `/api/cas/aba` 现场复现了两种结局。

### park/unpark 与 wait/notify 的区别？

- wait/notify 必须持有对象锁、不能指定唤醒哪个线程、有严格的时序要求；
- park/unpark 是**线程级别**：无需锁、可精确唤醒指定线程、支持超时、且 `unpark` 可以先于 `park` 调用（许可证机制——多次 unpark 只累积一张许可证）。AQS 挂起/唤醒线程用的就是 `LockSupport.park/unpark`。

### volatile 的底层是怎么实现的？

volatile 两层语义：可见性 + 禁止指令重排（插入内存屏障）。JVM 内部就是用 `loadFence / storeFence / fullFence` 去实现；x86 上普通屏障基本免费，只有 StoreLoad 需要 `lock` 前缀指令。**注意 volatile 不保证原子性**，`i++` 仍要 CAS/锁。

### objectFieldOffset 有什么用？

获取字段在对象内的内存偏移，之后用 `getInt(对象, 偏移)` / `compareAndSwapInt` 直接操作，不再碰反射。`AtomicInteger` 就是这么实现的：静态块里算好 `value` 字段偏移，之后所有操作都走 Unsafe。`arrayBaseOffset + arrayIndexScale` 则用于数组直接寻址。

### allocateInstance 绕过构造器有什么风险？

对象字段全是类型默认值、final 字段无法赋值、构造器校验被绕过，对象可能处于非法状态。反序列化攻击（如 2015 年 Fastjson 高危漏洞）就是利用了这类绕过，所以现代框架会做类白名单校验。

### 堆外内存是什么？有什么坑？

`allocateMemory` 分配的堆外内存不在 JVM 堆里、不参与 GC、必须手动 `freeMemory`。Netty 的 `PooledByteBufAllocator`、Kafka 用它做零拷贝缓冲。坑：忘了释放就是永久泄漏，长时间运行被系统 OOM 杀掉，且堆指标监控还看不到。

### Unsafe 会被移除吗？有没有官方替代？

短期不会（JDK 内部还在用），但官方口径是"不该用"。JDK 9 起引入 **VarHandle**（JEP 193）：类型安全、可移植的官方替代，支持 CAS、屏障、偏移量操作；JDK 9+ 的 `ConcurrentHashMap` 已改用 VarHandle。新代码请直接学 VarHandle。

## 本质

**Unsafe 为什么存在？** 因为 JDK 自己需要这些底层能力：并发库要 CAS、NIO/网络库要堆外内存、序列化要绕过构造器——这些在 Java 语言层面表达不了，又不能每次走 native + 反射（太慢）。于是 JDK 内部保留了这个"万能钥匙"。

**怎么理解它？** 把 Java 比作一栋有门禁的写字楼（语法糖、自动内存管理、安全检查），Unsafe 就是物业留的万能钥匙：关键时刻能开锁修东西，但也意味着谁拿了它都能进任何房间。所以框架作者用它造高性能组件，普通业务代码则应该绕开它。

**学习它的意义：** 看懂"为什么快"。面试聊框架原理时，说出"`ConcurrentHashMap` 的 Node 数组用 CAS 扩容、`AtomicInteger` 用字段偏移直接读写、`ReentrantLock` 用 `LockSupport.park` 挂起线程、Netty 用 Unsafe 分配堆外内存"，就真正懂底层了。

## 危险红线

| 危险 | 后果 | 正确姿势 |
| --- | --- | --- |
| 越界访问内存 | JVM 直接 SIGSEGV 崩溃，不抛 Java 异常 | 绝不写非法地址 |
| 忘掉 freeMemory | 堆外内存永久泄漏，被系统 OOM 杀掉 | 分配/释放成对出现，finally 里释放 |
| 用偏移量写字段 | 破坏封装与不变量，对象状态非法 | 正常走 getter/setter |
| 依赖内部实现 | 换 JDK 版本就崩，不可移植 | 用 VarHandle / JUC / 官方 API |

## 推荐实验顺序

1. 启动后端与前端，先看 **01 初识** 的能力地图，点「getUnsafe() 正规入口演示」看它为什么被堵死。
2. **02 堆外内存**：分配 5 个 int 看读写结果 → setMemory/copyMemory 十六进制 → 分配 5 块 1MB 观察"堆几乎不变"。
3. **03 绕过构造器**：看 countAfterUnsafe 没变、final 字段是 0，理解"校验被绕过"的危险。
4. **04 CAS**：自旋计数器（单线程 attempts=times）→ 三种自增性能对比 → **ABA 现场复现**（重点！两次结果对比看版本号的作用）。
5. **05 字段偏移**：看对象头 12 字节 → 打破封装（42→999）→ 数组直接寻址。
6. **06 park/unpark**：看时间线里"提前 unpark 后 park 立即返回"。
7. **07 内存屏障**：对照代码理解 volatile 的底层。
8. **08 危险与本质**：通读四大风险与本质，记住"生产代码远离 Unsafe"的铁律。

## 参考

- [JDK 源码 sun.misc.Unsafe](https://github.com/openjdk/jdk/blob/master/src/jdk.unsupported/share/classes/sun/misc/Unsafe.java)
- [JEP 193：Variable Handles（VarHandle 官方替代）](https://openjdk.org/jeps/193)
- [JEP 290：反序列化过滤（缓解反序列化攻击）](https://openjdk.org/jeps/290)
- [The JSR-133 Cookbook（JMM 内存屏障详解）](https://gee.cs.oswego.edu/dl/jmm/cookbook.html)
- [Java Memory Model（JMM 规范）](https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html)
