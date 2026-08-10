package com.example.excel.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Excel 下载响应工具。
 *
 * 封装「把 xlsx 字节流 + 中文文件名」构造成带正确响应头的 ResponseEntity：
 * - Content-Disposition 里同时给 ASCII 兼容的 filename 与 RFC 5987 的 filename*，
 *   保证 Chrome / Firefox / Safari / 老浏览器都能正确识别中文文件名；
 * - 返回 XLSX 官方 MIME 类型，而不是通用的 application/octet-stream。
 *
 * 这也是面试常问的「文件下载中文名乱码怎么解决」的正确答案。
 */
public final class ExcelWebSupport {

    /** .xlsx 文件的官方 MIME 类型 */
    public static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ExcelWebSupport() {
    }

    /**
     * 构造一个「下载 xlsx」的响应。
     *
     * @param filename 文件名（可含中文，如「员工名单.xlsx」）
     * @param data     xlsx 字节流
     */
    public static ResponseEntity<byte[]> xlsx(String filename, byte[] data) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filename))
                .contentType(XLSX_MEDIA_TYPE)
                .contentLength(data.length)
                .body(data);
    }

    /**
     * Content-Disposition：
     *   attachment; filename="encoded"; filename*=UTF-8''encoded
     * filename* 是 RFC 5987 标准写法，URLEncoder 保证中文/空格不破坏响应头。
     */
    public static String buildContentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;
    }
}
