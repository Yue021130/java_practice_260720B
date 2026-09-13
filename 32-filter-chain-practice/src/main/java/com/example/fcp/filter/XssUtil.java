package com.example.fcp.filter;

/**
 * XSS 转义工具：对请求参数中的 HTML 特殊字符做转义，防止存储型/反射型 XSS。
 *
 * <p>纯静态工具类，不依赖任何框架，便于单元测试。
 * 转义规则与 ESAPI/JSTL c:out 一致：&lt; &gt; &amp; &quot; &#x27; &#x28; &#x29;。</p>
 */
public class XssUtil {

    private XssUtil() {
    }

    /**
     * 对输入字符串做 XSS 转义；null/空串原样返回。
     */
    public static String clean(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                case '(':
                    sb.append("&#x28;");
                    break;
                case ')':
                    sb.append("&#x29;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
