package com.example.nf.service;

import com.example.nf.config.NioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NIO.2 文件操作服务。
 *
 * <p>封装 {@link java.nio.file.Path} 与 {@link java.nio.file.Files} 的常用 API，
 * 覆盖微信公众号原文所有示例：路径运算、创建、读写、复制移动、遍历。</p>
 *
 * <p>安全设计：</p>
 * <ul>
 *     <li>所有路径都解析到 {@code nio.work-dir} 沙箱目录内；</li>
 *     <li>normalize 后必须以沙箱目录为前缀，否则抛 SecurityException，防止路径遍历；</li>
 *     <li>所有返回给前端的字符串统一使用 {@code /} 分隔符，避免 Windows 反斜杠造成 JSON 转义问题。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NioFileService {

    private final NioProperties nioProperties;

    private Path workDir;

    @PostConstruct
    public void init() throws IOException {
        this.workDir = Paths.get(nioProperties.getWorkDir()).toAbsolutePath().normalize();
        if (!Files.exists(workDir)) {
            Files.createDirectories(workDir);
        }
        log.info("[NIO.2] 文件操作沙箱目录: {}", workDir);
    }

    /**
     * 将相对路径解析为沙箱内的绝对路径，并做路径遍历校验。
     */
    private Path resolveSandbox(String path) {
        if (path == null || path.trim().isEmpty()) {
            return workDir;
        }
        Path resolved = workDir.resolve(path).toAbsolutePath().normalize();
        if (!resolved.startsWith(workDir)) {
            throw new SecurityException("路径越界，不允许访问沙箱外目录: " + path);
        }
        return resolved;
    }

    /**
     * 两个路径都解析并校验是否都在沙箱内（用于 relativize）。
     */
    private Path[] resolvePair(String first, String second) {
        Path a = resolveSandbox(first);
        Path b = resolveSandbox(second);
        return new Path[]{a, b};
    }

    private String toSlash(Path path) {
        return path.toString().replace('\\', '/');
    }

    // ==================== Path 四兄弟 ====================

    /**
     * resolve：把子路径拼到基路径上。
     */
    public Map<String, String> resolve(String base, String other) {
        Path basePath = resolveSandbox(base);
        Path result = basePath.resolve(other);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("base", toSlash(basePath));
        map.put("other", other);
        map.put("result", toSlash(result));
        map.put("tip", "resolve 本质是把参数拼到基路径后面；如果参数是绝对路径，则直接返回参数");
        return map;
    }

    /**
     * resolveSibling：替换兄弟节点。
     */
    public Map<String, String> resolveSibling(String path, String sibling) {
        Path src = resolveSandbox(path);
        Path result = src.resolveSibling(sibling);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("source", toSlash(src));
        map.put("sibling", sibling);
        map.put("result", toSlash(result));
        map.put("tip", "resolveSibling 用于同目录改名/换文件，如 a.txt -> b.txt");
        return map;
    }

    /**
     * relativize：求从 from 到 to 的相对路径。
     */
    public Map<String, String> relativize(String from, String to) {
        Path[] pair = resolvePair(from, to);
        Path result = pair[0].relativize(pair[1]);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("from", toSlash(pair[0]));
        map.put("to", toSlash(pair[1]));
        map.put("result", toSlash(result));
        map.put("tip", "relativize 要求两个路径同为绝对或同为相对；结果不是简单去前缀，文件名也参与计算");
        return map;
    }

    /**
     * normalize：清理路径中的 . 和 ..。
     */
    public Map<String, String> normalize(String path) {
        Path src = resolveSandbox(path);
        Path result = src.normalize();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("source", toSlash(src));
        map.put("result", toSlash(result));
        map.put("tip", "normalize 是纯字符串运算，不碰磁盘，比 File.getCanonicalPath() 更轻量");
        return map;
    }

    /**
     * Path 与 File 互转。
     */
    public Map<String, String> toAndFromFile(String relativePath) {
        Path path = resolveSandbox(relativePath);
        File file = path.toFile();
        Path back = file.toPath();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("path", toSlash(path));
        map.put("file", toSlash(file.toPath()));
        map.put("pathFromFile", toSlash(back));
        map.put("equals", String.valueOf(toSlash(path).equals(toSlash(back))));
        map.put("tip", "Path.toFile() 与 File.toPath() 只是视图转换，底层指向同一路径");
        return map;
    }

    // ==================== Files 创建 ====================

    public enum CreateType {
        FILE, DIRECTORY, TEMP_FILE, TEMP_DIR
    }

    /**
     * 创建文件/目录/临时文件/临时目录。
     */
    public Map<String, Object> create(String relativePath, CreateType type, String prefix, String suffix) throws IOException {
        Path target;
        switch (type) {
            case FILE:
                target = resolveSandbox(relativePath);
                Path parent = target.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(target);
                break;
            case DIRECTORY:
                target = resolveSandbox(relativePath);
                Files.createDirectories(target);
                break;
            case TEMP_FILE:
                Path tmpDir = relativePath == null || relativePath.isEmpty() ? workDir : resolveSandbox(relativePath);
                target = Files.createTempFile(tmpDir, prefix == null ? "tmp-" : prefix, suffix == null ? ".tmp" : suffix);
                break;
            case TEMP_DIR:
                Path tmpDir2 = relativePath == null || relativePath.isEmpty() ? workDir : resolveSandbox(relativePath);
                target = Files.createTempDirectory(tmpDir2, prefix == null ? "tmpdir-" : prefix);
                break;
            default:
                throw new IllegalArgumentException("不支持的创建类型: " + type);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name());
        map.put("path", toSlash(target));
        map.put("exists", Files.exists(target));
        map.put("isDirectory", Files.isDirectory(target));
        return map;
    }

    /**
     * 删除文件或目录。
     */
    public Map<String, Object> delete(String relativePath, boolean recursive) throws IOException {
        Path target = resolveSandbox(relativePath);
        if (recursive && Files.isDirectory(target)) {
            deleteRecursively(target);
        } else {
            Files.deleteIfExists(target);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(target));
        map.put("exists", Files.exists(target));
        return map;
    }

    // ==================== Files 读写 ====================

    /**
     * 写文本，覆盖写。
     */
    public Map<String, Object> writeText(String relativePath, String content) throws IOException {
        Path target = resolveSandbox(relativePath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        return properties(relativePath);
    }

    /**
     * 读全部文本。
     */
    public Map<String, Object> readText(String relativePath) throws IOException {
        Path target = resolveSandbox(relativePath);
        byte[] bytes = Files.readAllBytes(target);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(target));
        map.put("content", new String(bytes, StandardCharsets.UTF_8));
        map.put("size", bytes.length);
        return map;
    }

    /**
     * 按行读取。
     */
    public Map<String, Object> readLines(String relativePath) throws IOException {
        Path target = resolveSandbox(relativePath);
        List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(target));
        map.put("lineCount", lines.size());
        map.put("lines", lines);
        return map;
    }

    /**
     * 流式按行处理并统计。
     */
    public Map<String, Object> lineStats(String relativePath) throws IOException {
        Path target = resolveSandbox(relativePath);
        long nonEmptyCount;
        long totalChars;
        try (Stream<String> stream = Files.lines(target, StandardCharsets.UTF_8)) {
            nonEmptyCount = stream.filter(s -> !s.isEmpty()).count();
        }
        try (Stream<String> stream = Files.lines(target, StandardCharsets.UTF_8)) {
            totalChars = stream.mapToLong(String::length).sum();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(target));
        map.put("nonEmptyLineCount", nonEmptyCount);
        map.put("totalChars", totalChars);
        map.put("tip", "Files.lines 返回 Stream，适合大文件懒加载，必须用 try-with-resources 关闭");
        return map;
    }

    /**
     * 使用 BufferedWriter 写多行。
     */
    public Map<String, Object> writeWithBufferedWriter(String relativePath, List<String> lines) throws IOException {
        Path target = resolveSandbox(relativePath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        return readLines(relativePath);
    }

    /**
     * 使用 BufferedReader 读全部行。
     */
    public Map<String, Object> readWithBufferedReader(String relativePath) throws IOException {
        Path target = resolveSandbox(relativePath);
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(target));
        map.put("lines", lines);
        return map;
    }

    /**
     * 写 Properties 文件。
     */
    public Map<String, Object> writeProperties(String relativePath, Map<String, String> props) throws IOException {
        Path target = resolveSandbox(relativePath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.putAll(props);
        try (OutputStream os = Files.newOutputStream(target)) {
            properties.store(os, "Generated by NIO.2 practice");
        }
        return readProperties(relativePath);
    }

    /**
     * 读 Properties 文件。
     */
    public Map<String, Object> readProperties(String relativePath) throws IOException {
        Path target = resolveSandbox(relativePath);
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(target)) {
            properties.load(is);
        }
        Map<String, String> result = new LinkedHashMap<>();
        properties.stringPropertyNames().forEach(k -> result.put(k, properties.getProperty(k)));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(target));
        map.put("properties", result);
        return map;
    }

    // ==================== 复制与移动 ====================

    /**
     * 复制文件。
     */
    public Map<String, Object> copy(String src, String dst, boolean replaceExisting, boolean copyAttributes) throws IOException {
        Path srcPath = resolveSandbox(src);
        Path dstPath = resolveSandbox(dst);
        Path parent = dstPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        List<CopyOption> options = new ArrayList<>();
        if (replaceExisting) {
            options.add(StandardCopyOption.REPLACE_EXISTING);
        }
        if (copyAttributes) {
            options.add(StandardCopyOption.COPY_ATTRIBUTES);
        }
        Files.copy(srcPath, dstPath, options.toArray(new CopyOption[0]));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("src", toSlash(srcPath));
        map.put("dst", toSlash(dstPath));
        map.put("exists", Files.exists(dstPath));
        map.put("tip", "Files.copy 复制目录只会复制空目录本身，内部文件不会递归复制");
        return map;
    }

    /**
     * 复制整个目录（递归）。
     */
    public Map<String, Object> copyDirectory(String srcDir, String dstDir) throws IOException {
        Path src = resolveSandbox(srcDir);
        Path dst = resolveSandbox(dstDir);
        if (!Files.isDirectory(src)) {
            throw new IllegalArgumentException("源路径不是目录: " + srcDir);
        }
        try (Stream<Path> stream = Files.walk(src)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                Path target = dst.resolve(src.relativize(p));
                if (Files.isDirectory(p)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("src", toSlash(src));
        map.put("dst", toSlash(dst));
        map.put("exists", Files.exists(dst));
        map.put("tip", "完整目录复制需要 Files.walk + resolve + relativize 自己实现");
        return map;
    }

    /**
     * 移动/重命名文件。
     */
    public Map<String, Object> move(String src, String dst, boolean atomic) throws IOException {
        Path srcPath = resolveSandbox(src);
        Path dstPath = resolveSandbox(dst);
        List<CopyOption> options = new ArrayList<>();
        options.add(StandardCopyOption.REPLACE_EXISTING);
        if (atomic) {
            options.add(StandardCopyOption.ATOMIC_MOVE);
        }
        Files.move(srcPath, dstPath, options.toArray(new CopyOption[0]));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("src", toSlash(srcPath));
        map.put("dst", toSlash(dstPath));
        map.put("srcExists", Files.exists(srcPath));
        map.put("dstExists", Files.exists(dstPath));
        map.put("tip", "ATOMIC_MOVE 只在同一个文件系统内支持，跨盘会抛 AtomicMoveNotSupportedException");
        return map;
    }

    /**
     * 从输入流复制到沙箱（模拟文件上传落地）。
     */
    public Map<String, Object> upload(InputStream in, String dst) throws IOException {
        Path dstPath = resolveSandbox(dst);
        Path parent = dstPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.copy(in, dstPath, StandardCopyOption.REPLACE_EXISTING);
        return propertiesByPath(dstPath);
    }

    // ==================== 遍历 ====================

    /**
     * list：只列出当前目录下一层。
     */
    public Map<String, Object> list(String relativeDir) throws IOException {
        Path dir = resolveSandbox(relativeDir);
        List<Map<String, String>> items = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("name", p.getFileName().toString());
                item.put("type", Files.isDirectory(p) ? "DIRECTORY" : "FILE");
                item.put("path", toSlash(workDir.relativize(p)));
                items.add(item);
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dir", toSlash(dir));
        map.put("items", items);
        map.put("tip", "Files.list 返回 Stream，必须用 try-with-resources 关闭，否则 Windows 会占句柄");
        return map;
    }

    /**
     * walk：递归遍历，可限制深度。
     */
    public Map<String, Object> walk(String relativeDir, int maxDepth) throws IOException {
        Path dir = resolveSandbox(relativeDir);
        List<Map<String, String>> files = new ArrayList<>();
        try (Stream<Path> stream = maxDepth <= 0 ? Files.walk(dir) : Files.walk(dir, maxDepth)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("relative", toSlash(workDir.relativize(p)));
                item.put("size", String.valueOf(sizeOf(p)));
                files.add(item);
            });
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dir", toSlash(dir));
        map.put("maxDepth", maxDepth <= 0 ? "无限制" : maxDepth);
        map.put("files", files);
        map.put("tip", "Files.walk 也返回 Stream，必须关闭；maxDepth <= 0 表示不限制深度");
        return map;
    }

    /**
     * walkFileTree：统计目录下文件数与总大小。
     */
    public Map<String, Object> walkFileTreeStats(String relativeDir) throws IOException {
        Path dir = resolveSandbox(relativeDir);
        AtomicLong count = new AtomicLong();
        AtomicLong total = new AtomicLong();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                count.incrementAndGet();
                total.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dir", toSlash(dir));
        map.put("fileCount", count.get());
        map.put("totalBytes", total.get());
        map.put("tip", "walkFileTree + SimpleFileVisitor 适合边走边做复杂操作（统计、删除、过滤）");
        return map;
    }

    /**
     * 递归删除目录。
     */
    public Map<String, Object> deleteRecursively(String relativeDir) throws IOException {
        Path dir = resolveSandbox(relativeDir);
        deleteRecursively(dir);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dir", toSlash(dir));
        map.put("exists", Files.exists(dir));
        map.put("tip", "递归删除使用 walkFileTree，先在 visitFile 删文件，再在 postVisitDirectory 删目录");
        return map;
    }

    private void deleteRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ==================== 属性 ====================

    /**
     * 文件/目录属性。
     */
    public Map<String, Object> properties(String relativePath) throws IOException {
        return propertiesByPath(resolveSandbox(relativePath));
    }

    private Map<String, Object> propertiesByPath(Path path) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", toSlash(path));
        map.put("exists", Files.exists(path));
        if (Files.exists(path)) {
            map.put("size", Files.size(path));
            map.put("isDirectory", Files.isDirectory(path));
            map.put("isRegularFile", Files.isRegularFile(path));
            map.put("lastModified", Files.getLastModifiedTime(path).toString());
        }
        return map;
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }
}
