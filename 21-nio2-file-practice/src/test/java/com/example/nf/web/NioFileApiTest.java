package com.example.nf.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NioFileController 接口集成测试。
 *
 * <p>使用 MockMvc 验证 REST 路径、参数绑定、统一响应体以及沙箱安全。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class NioFileApiTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("nio.work-dir", () -> tempDir.toAbsolutePath().toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pathResolve_shouldReturnJoinedPath() throws Exception {
        mockMvc.perform(get("/api/nio/path/resolve")
                        .param("base", "docs")
                        .param("other", "a.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.result").value(org.hamcrest.Matchers.endsWith("docs/a.txt")));
    }

    @Test
    void fileCreate_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/nio/file/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"api-created.txt\",\"type\":\"FILE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    void fileWriteAndRead_shouldRoundTrip() throws Exception {
        mockMvc.perform(post("/api/nio/file/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"api-write.txt\",\"content\":\"hello api\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true));

        mockMvc.perform(get("/api/nio/file/read")
                        .param("path", "api-write.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("hello api"));
    }

    @Test
    void upload_shouldSaveMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "multipart content".getBytes());

        mockMvc.perform(multipart("/api/nio/file/upload")
                        .file(file)
                        .param("dst", "uploaded-api.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    void traversal_shouldListAndWalk() throws Exception {
        mockMvc.perform(post("/api/nio/file/write")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"list/a.txt\",\"content\":\"a\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/nio/file/list").param("dir", "list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(get("/api/nio/file/walk").param("dir", "list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.files.length()").value(1));
    }

    @Test
    void pathTraversal_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/nio/file/read").param("path", "../secret.txt"))
                .andExpect(status().isOk()) // 全局异常处理包装后 HTTP 200，业务 code 403
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void explain_shouldReturnEightLegEssay() throws Exception {
        mockMvc.perform(get("/api/nio/explain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").exists())
                .andExpect(jsonPath("$.data.path.resolve").exists());
    }
}
