package com.example.fcp.filter;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 清洗包装器：重写参数读取方法，返回转义后的值。
 *
 * <p>继承 {@link RepeatedlyReadHttpServletRequestWrapper} 同时获得
 * body 缓存能力——XssFilter 读取 body 清洗后，Controller 的 @RequestBody 仍可正常解析。</p>
 */
public class XssHttpServletRequestWrapper extends RepeatedlyReadHttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws java.io.IOException {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        return XssUtil.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = XssUtil.clean(values[i]);
        }
        return cleaned;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> cleaned = new HashMap<>(original.size() * 2);
        for (Map.Entry<String, String[]> entry : original.entrySet()) {
            String[] values = entry.getValue();
            String[] cleanedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanedValues[i] = XssUtil.clean(values[i]);
            }
            cleaned.put(entry.getKey(), cleanedValues);
        }
        return cleaned;
    }

    @Override
    public BufferedReader getReader() {
        BufferedReader reader = super.getReader();
        // body 清洗：包装 reader，逐行转义（JSON 中的 < > 等被转义不影响解析）
        return new BufferedReader(new java.io.StringReader(XssUtil.clean(readAll(reader))));
    }

    private String readAll(BufferedReader reader) {
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取请求体失败", e);
        }
        return sb.toString();
    }
}
