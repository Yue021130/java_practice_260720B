package com.example.nf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NioFileService 单元测试。
 *
 * <p>使用临时目录作为沙箱，覆盖路径运算、创建、读写、复制移动、遍历、属性等核心方法。</p>
 */
@SpringBootTest
class NioFileServiceUnitTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("nio.work-dir", () -> tempDir.toAbsolutePath().toString());
    }

    @Autowired
    private NioFileService nioFileService;

    /**
     * 每个测试前清空沙箱，避免测试间文件互相影响。
     */
    @BeforeEach
    void cleanSandbox() throws IOException {
        if (Files.exists(tempDir)) {
            // 按路径深度倒序删除：先子节点后父节点
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
        Files.createDirectories(tempDir);
    }

    @Test
    void resolve_shouldJoinPaths() {
        Map<String, String> result = nioFileService.resolve("docs", "report.txt");
        assertThat(result.get("other")).isEqualTo("report.txt");
        assertThat(result.get("result")).endsWith("docs/report.txt").contains("/");
    }

    @Test
    void normalize_shouldCleanDots() {
        Map<String, String> result = nioFileService.normalize("a/../b/./c.txt");
        assertThat(result.get("result")).doesNotContain("/../").doesNotContain("/./");
    }

    @Test
    void resolveSandbox_shouldRejectTraversal() {
        assertThatThrownBy(() -> nioFileService.properties("../secret.txt"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("路径越界");
    }

    @Test
    void createAndReadText_shouldWork() throws IOException {
        nioFileService.create("hello.txt", NioFileService.CreateType.FILE, null, null);
        nioFileService.writeText("hello.txt", "你好 NIO.2");

        Map<String, Object> read = nioFileService.readText("hello.txt");
        assertThat(read.get("content")).isEqualTo("你好 NIO.2");

        Map<String, Object> props = nioFileService.properties("hello.txt");
        assertThat(props.get("exists")).isEqualTo(true);
        assertThat(props.get("isRegularFile")).isEqualTo(true);
    }

    @Test
    void writeAndReadLines_shouldWork() throws IOException {
        List<String> lines = Arrays.asList("第一行", "", "第三行");
        nioFileService.writeWithBufferedWriter("lines.txt", lines);

        Map<String, Object> result = nioFileService.readLines("lines.txt");
        @SuppressWarnings("unchecked")
        List<String> actual = (List<String>) result.get("lines");
        assertThat(actual).hasSize(3);
        assertThat(result.get("lineCount")).isEqualTo(3);

        Map<String, Object> stats = nioFileService.lineStats("lines.txt");
        assertThat(stats.get("nonEmptyLineCount")).isEqualTo(2L);
    }

    @Test
    void copyAndMove_shouldWork() throws IOException {
        nioFileService.writeText("src.txt", "copy me");
        nioFileService.copy("src.txt", "dst.txt", true, false);

        Map<String, Object> read = nioFileService.readText("dst.txt");
        assertThat(read.get("content")).isEqualTo("copy me");

        nioFileService.move("dst.txt", "moved.txt", false);
        Map<String, Object> moved = nioFileService.properties("moved.txt");
        assertThat(moved.get("exists")).isEqualTo(true);
    }

    @Test
    void copyDirectory_shouldCopyRecursively() throws IOException {
        nioFileService.create("dir/a", NioFileService.CreateType.DIRECTORY, null, null);
        nioFileService.writeText("dir/a/1.txt", "one");
        nioFileService.create("dir/b", NioFileService.CreateType.DIRECTORY, null, null);
        nioFileService.writeText("dir/b/2.txt", "two");

        nioFileService.copyDirectory("dir", "dir-copy");

        Map<String, Object> walk = nioFileService.walk("dir-copy", 0);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> files = (List<Map<String, String>>) walk.get("files");
        assertThat(files).hasSize(2);
    }

    @Test
    void listAndWalk_shouldWork() throws IOException {
        nioFileService.writeText("root.txt", "root");
        nioFileService.create("sub", NioFileService.CreateType.DIRECTORY, null, null);
        nioFileService.writeText("sub/nested.txt", "nested");

        Map<String, Object> list = nioFileService.list(null);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> items = (List<Map<String, String>>) list.get("items");
        assertThat(items).hasSize(2);

        Map<String, Object> walk = nioFileService.walk(null, 0);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> files = (List<Map<String, String>>) walk.get("files");
        assertThat(files).hasSize(2);
    }

    @Test
    void upload_shouldCopyFromStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("upload content".getBytes(StandardCharsets.UTF_8));
        nioFileService.upload(in, "uploaded.txt");

        Map<String, Object> read = nioFileService.readText("uploaded.txt");
        assertThat(read.get("content")).isEqualTo("upload content");
    }

    @Test
    void propertiesFile_shouldRoundTrip() throws IOException {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("app.name", "nio-practice");
        props.put("app.version", "1.0");

        nioFileService.writeProperties("app.properties", props);
        Map<String, Object> read = nioFileService.readProperties("app.properties");
        @SuppressWarnings("unchecked")
        Map<String, String> actual = (Map<String, String>) read.get("properties");
        assertThat(actual).containsEntry("app.name", "nio-practice").containsEntry("app.version", "1.0");
    }

    @Test
    void walkFileTreeStats_shouldCountFilesAndBytes() throws IOException {
        nioFileService.writeText("f1.txt", "abc");
        nioFileService.writeText("f2.txt", "de");

        Map<String, Object> stats = nioFileService.walkFileTreeStats(null);
        assertThat(stats.get("fileCount")).isEqualTo(2L);
        assertThat(stats.get("totalBytes")).isEqualTo(5L);
    }

    @Test
    void deleteRecursive_shouldRemoveDirectory() throws IOException {
        nioFileService.create("to-delete/a", NioFileService.CreateType.DIRECTORY, null, null);
        nioFileService.writeText("to-delete/a/x.txt", "x");

        nioFileService.delete("to-delete", true);
        assertThat(Files.exists(tempDir.resolve("to-delete"))).isFalse();
    }
}
