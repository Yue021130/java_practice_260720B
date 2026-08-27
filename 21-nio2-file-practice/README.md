# 21-nio2-file-practice：Java NIO.2 Path + Files 文件操作实战

基于微信公众号原文《Java NIO.2 文件操作：Path ＋ Files ，告别 File 和第三方工具类》的 Spring Boot + Vue3 实战复现。

---

## 一、项目定位

真实业务里，文件上传、日志归档、配置读取、目录遍历几乎绕不开 NIO.2 的 `java.nio.file.Path` 与 `java.nio.file.Files`。

本章节把原文所有示例封装成 **REST 接口 + 可视化前端面板**，你可以：

- 在线实验 Path 四兄弟（resolve / resolveSibling / relativize / normalize）。
- 创建、读写、复制、移动、删除文件与目录。
- 用 Stream 按行处理大文件，用 Properties 做配置落地。
- 看八股速记，理解面试常考的考点与坑点。

---

## 二、技术栈

| 层 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 后端 | Spring Boot | 2.7.18 | Java 8 兼容，学习门槛低 |
| 后端 | Java | 1.8 | 使用 NIO.2（Java 7+） |
| 后端 | Lombok | 由父工程管理 | 减少样板代码 |
| 后端 | springdoc-openapi-ui | 1.7.0 | 自动生成 Swagger 文档 |
| 前端 | Vue | 3.4.21 | Composition API |
| 前端 | Vite | 5.2.8 | 构建工具 |
| 前端 | axios | 1.6.8 | HTTP 请求 |

---

## 三、快速启动

### 3.1 启动后端

```bash
cd 21-nio2-file-practice
mvn spring-boot:run
```

后端默认端口 `8101`，沙箱目录 `./nio-work`（可在 `application.yml` 中修改）。

接口文档：http://localhost:8101/swagger-ui.html

### 3.2 启动前端

```bash
cd 21-nio2-file-practice/web
npm install
npm run dev
```

前端默认端口 `5194`，已配置代理到 `http://localhost:8101`。

---

## 四、核心八股

### 4.1 Path 四兄弟

| 方法 | 作用 | 面试要点 |
|---|---|---|
| `resolve(String other)` | 把 other 拼到当前路径后 | other 是绝对路径则直接返回 other |
| `resolveSibling(String other)` | 替换同一目录下的兄弟节点 | 常用于文件改名 `a.txt -> b.txt` |
| `relativize(Path other)` | 求从当前路径到 other 的相对路径 | 两路径必须同为绝对或同为相对 |
| `normalize()` | 清理 `.` 和 `..` | 纯字符串运算，不访问磁盘 |

### 4.2 Files 核心方法

| 分类 | 方法 | 说明 |
|---|---|---|
| 创建 | `createFile` / `createDirectory` / `createTempFile` / `createTempDirectory` | 创建文件、目录、临时文件/目录 |
| 删除 | `delete` / `deleteIfExists` | 删除文件或空目录 |
| 读 | `readAllBytes` / `readAllLines` / `newBufferedReader` / `newInputStream` | 小文件一次性读，大文件流式读 |
| 写 | `write` / `newBufferedWriter` / `newOutputStream` | 覆盖写或流式写 |
| 复制 | `copy(Path, Path, CopyOption...)` | 只复制文件；复制目录只复制空目录本身 |
| 移动 | `move(Path, Path, CopyOption...)` | `ATOMIC_MOVE` 同文件系统内原子 |
| 遍历 | `list` / `walk` / `walkFileTree` | list 只一层；walk 递归；walkFileTree 可自定义访问器 |
| 属性 | `exists` / `isDirectory` / `isRegularFile` / `size` / `getLastModifiedTime` | 常用属性判断 |

### 4.3 高频面试题

**Q1：Path 和 File 有什么区别？**

- `Path` 是 NIO.2 引入的不可变、平台无关路径抽象；`File` 是旧 IO 的类。
- `Path` 支持符号链接、文件属性、WatchService 等高级特性。
- `Path.toFile()` 与 `File.toPath()` 只是视图转换，底层指向同一路径。

**Q2：Files.copy 复制目录会递归吗？**

不会。复制目录只会复制空目录本身。完整目录复制需要 `Files.walk` + `resolve` + `relativize` 自己实现。

**Q3：Files.lines 和 BufferedReader 怎么选？**

- `Files.lines` 返回 `Stream<String>`，适合链式处理与函数式操作，但必须用 try-with-resources 关闭。
- `BufferedReader` 是老牌流式读取，适合逐行控制逻辑。

**Q4：ATOMIC_MOVE 一定能原子移动吗？**

不一定。`StandardCopyOption.ATOMIC_MOVE` 只在同一个文件系统内支持，跨盘会抛 `AtomicMoveNotSupportedException`。

**Q5：如何防止路径遍历攻击？**

1. 把用户输入的相对路径解析到业务沙箱目录。
2. `normalize()` 后校验是否以沙箱目录为前缀。
3. 拒绝 `../` 等越界路径。

---

## 五、接口清单

### 5.1 Path 路径运算

| 方法 | 路径 | 参数 |
|---|---|---|
| GET | `/api/nio/path/resolve` | `base`, `other` |
| GET | `/api/nio/path/resolveSibling` | `path`, `sibling` |
| GET | `/api/nio/path/relativize` | `from`, `to` |
| GET | `/api/nio/path/normalize` | `path` |
| GET | `/api/nio/path/to-and-from-file` | `path` |

### 5.2 创建与删除

| 方法 | 路径 | 请求体 |
|---|---|---|
| POST | `/api/nio/file/create` | `{path, type, prefix, suffix}` |
| POST | `/api/nio/file/delete` | `{path, recursive}` |

### 5.3 读写

| 方法 | 路径 | 参数 / 请求体 |
|---|---|---|
| POST | `/api/nio/file/write` | `{path, content}` |
| GET | `/api/nio/file/read` | `path` |
| GET | `/api/nio/file/lines` | `path` |
| GET | `/api/nio/file/lineStats` | `path` |
| POST | `/api/nio/file/writeLines` | `{path, lines}` |
| GET | `/api/nio/file/readLinesBuffered` | `path` |

### 5.4 Properties

| 方法 | 路径 | 请求体 / 参数 |
|---|---|---|
| POST | `/api/nio/file/writeProperties` | `{path, properties}` |
| GET | `/api/nio/file/readProperties` | `path` |

### 5.5 复制与移动

| 方法 | 路径 | 请求体 |
|---|---|---|
| POST | `/api/nio/file/copy` | `{src, dst, replaceExisting, copyAttributes}` |
| POST | `/api/nio/file/copyDirectory` | `{src, dst}` |
| POST | `/api/nio/file/move` | `{src, dst, atomic}` |
| POST | `/api/nio/file/upload` | multipart `file`, `dst` |

### 5.6 遍历

| 方法 | 路径 | 参数 |
|---|---|---|
| GET | `/api/nio/file/list` | `dir` |
| GET | `/api/nio/file/walk` | `dir`, `maxDepth` |
| GET | `/api/nio/file/stats` | `dir` |

### 5.7 属性与八股

| 方法 | 路径 | 参数 |
|---|---|---|
| GET | `/api/nio/file/properties` | `path` |
| GET | `/api/nio/explain` | 无 |

---

## 六、目录结构

```
21-nio2-file-practice/
├── src/main/java/com/example/nf/
│   ├── Nio2FilePracticeApplication.java   # 启动类
│   ├── common/                             # 统一响应、异常处理、CORS、OpenAPI
│   │   ├── ApiResponse.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── CorsConfig.java
│   │   └── OpenApiConfig.java
│   ├── config/
│   │   └── NioProperties.java              # nio.work-dir 配置
│   ├── dto/                                # 请求 DTO
│   ├── service/
│   │   └── NioFileService.java             # 核心文件操作服务
│   └── web/
│       └── NioFileController.java          # REST 接口
├── src/test/java/com/example/nf/
│   ├── service/NioFileServiceUnitTest.java
│   └── web/NioFileApiTest.java
├── src/main/resources/application.yml
├── web/                                    # Vue3 前端
│   ├── src/
│   │   ├── main.js
│   │   ├── App.vue
│   │   └── api/nio.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── pom.xml
└── README.md
```

---

## 七、与公众号原文对照

原文本地备份：`../21/Java NIO.2 文件操作：Path ＋ Files ，告别 File 和第三方工具类.html`

| 原文知识点 | 本项目对应实现 |
|---|---|
| Path.resolve / resolveSibling / relativize / normalize | `NioFileService` Path 四兄弟 + `/api/nio/path/*` 接口 |
| Files.createFile / createDirectory / createTempFile / createTempDirectory | `NioFileService.create(...)` + `/api/nio/file/create` |
| Files.delete / deleteIfExists | `NioFileService.delete(...)` + `/api/nio/file/delete` |
| Files.readAllBytes / readAllLines / newBufferedReader / newBufferedWriter | `readText` / `readLines` / `writeWithBufferedWriter` 等 |
| Files.newInputStream / newOutputStream + Properties | `readProperties` / `writeProperties` |
| Files.copy / move | `copy` / `move` 接口 |
| 目录复制 | `copyDirectory` 使用 `Files.walk` + `relativize` + `resolve` |
| Files.list / walk / walkFileTree | `list` / `walk` / `stats` 接口 |
| 文件属性 | `properties` 接口 |

---

## 八、测试

```bash
# 后端测试
cd 21-nio2-file-practice
mvn test

# 前端构建
cd 21-nio2-file-practice/web
npm install
npm run build
```

---

## 九、作者

由 Kimi Code CLI 协助生成，作者：我。
