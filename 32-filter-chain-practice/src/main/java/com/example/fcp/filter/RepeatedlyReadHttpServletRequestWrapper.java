package com.example.fcp.filter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取的 HttpServletRequest 包装器：把请求体读入内存字节数组，供多次读取。
 *
 * <p>八股：默认 HttpServletRequest 的 InputStream 只能读一次；Filter 或 Interceptor 中一旦读取，
 * 后续 Controller 的 @RequestBody 就会报错「stream closed」。
 * 因此要在 Filter 链最前端把 body 缓存到 wrapper，后续所有组件都改读 wrapper。</p>
 */
public class RepeatedlyReadHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RepeatedlyReadHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 按 request 实际编码读取；未设置则默认 UTF-8
        String encoding = request.getCharacterEncoding();
        if (encoding == null || encoding.isEmpty()) {
            encoding = StandardCharsets.UTF_8.name();
        }
        // Java 8 兼容：用 ByteArrayOutputStream 手动读取全部字节（readAllBytes 是 Java 9+）
        try (java.io.InputStream is = request.getInputStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            body = bos.toByteArray();
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // 同步读取，无需 listener
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /** 供日志打印 body 内容 */
    public String getBodyString() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
