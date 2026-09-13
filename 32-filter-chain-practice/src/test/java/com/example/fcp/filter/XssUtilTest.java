package com.example.fcp.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * XSS 转义工具单元测试。
 */
class XssUtilTest {

    @Test
    void nullAndEmpty() {
        assertNull(XssUtil.clean(null));
        assertEquals("", XssUtil.clean(""));
    }

    @Test
    void cleanTags() {
        assertEquals("&lt;script&gt;alert&#x28;1&#x29;&lt;/script&gt;",
                XssUtil.clean("<script>alert(1)</script>"));
        assertEquals("&lt;img src=1 onerror=alert&#x28;1&#x29;&gt;",
                XssUtil.clean("<img src=1 onerror=alert(1)>"));
    }

    @Test
    void cleanQuotesAndAmp() {
        assertEquals("&quot;hello&quot;", XssUtil.clean("\"hello\""));
        assertEquals("a=1&amp;b=2", XssUtil.clean("a=1&b=2"));
        assertEquals("&#x27;x&#x27;", XssUtil.clean("'x'"));
    }

    @Test
    void cleanBrackets() {
        assertEquals("&#x28;1&#x29;", XssUtil.clean("(1)"));
    }

    @Test
    void mixedTextPreserved() {
        // 普通文本原样保留，仅特殊字符转义
        assertEquals("会议通知：周五15:00开会，地点A&amp;B楼",
                XssUtil.clean("会议通知：周五15:00开会，地点A&B楼"));
    }
}
