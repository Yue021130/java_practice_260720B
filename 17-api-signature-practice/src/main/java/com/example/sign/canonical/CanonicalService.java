package com.example.sign.canonical;

import com.example.sign.signature.HmacSignService;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 07. 规范化：URI / QueryString / Headers 排序规则。
 *
 * 签名的前提是「两端拼出完全相同的字符串」。URL 参数顺序、头顺序、大小写、
 * 编码方式都会改变字符串，所以必须先规范化：query 按 key 排序、头按小写 key 排序、
 * URI 按固定编码规则处理。排序后，同一组数据无论以什么顺序发来，签名结果一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanonicalService {

    private final HmacSignService signService;
    private final SignLogStore logStore;

    /**
     * query 排序演示：同样的参数，乱序输入 vs 排序后，规范化结果完全一致。
     */
    public Map<String, Object> querySort() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> shuffled = new LinkedHashMap<>();
        shuffled.put("size", "20");
        shuffled.put("name", "zhang");
        shuffled.put("page", "1");

        Map<String, String> sorted = new LinkedHashMap<>();
        sorted.put("name", "zhang");
        sorted.put("page", "1");
        sorted.put("size", "20");

        String canonicalShuffled = signService.canonicalQueryString(shuffled);
        String canonicalSorted = signService.canonicalQueryString(sorted);

        result.put("input1", "size=20&name=zhang&page=1");
        result.put("input2", "name=zhang&page=1&size=20");
        result.put("canonical1", canonicalShuffled);
        result.put("canonical2", canonicalSorted);
        result.put("identical", canonicalShuffled.equals(canonicalSorted));
        result.put("tip", "乱序的两组参数规范化后都是「name=zhang&page=1&size=20」："
                + "不排序的话，客户端与服务器按不同顺序拼串，签名永远对不上。");

        logStore.add("canonical", "demo", canonicalShuffled.equals(canonicalSorted), "query 排序");
        return result;
    }

    /**
     * headers 排序演示：参与签名的头统一转小写按字典序拼接。
     */
    public Map<String, Object> headersSort() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Nonce", "nonce-123");
        headers.put("X-App-Id", "demo_app_001");
        headers.put("X-Timestamp", "1723537860");

        String canonical = signService.canonicalHeaders(headers);
        result.put("input", "X-Nonce / X-App-Id / X-Timestamp（声明顺序）");
        result.put("canonicalHeaders", canonical.replace("\n", "\\n"));
        result.put("sortedKeys", "x-app-id → x-nonce → x-timestamp（字典序）");
        result.put("tip", "头名统一小写并按字典序排：不管客户端按什么顺序设置头，服务端都能算出同一个串。");

        logStore.add("canonical", "demo", true, "headers 排序");
        return result;
    }

    /**
     * URI 规范化说明。
     */
    public Map<String, Object> uriEncoding() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rules", new String[]{
                "路径按 RFC 3986 编码：保留字符（:/?#[]@!$&'()*+,;=）不编码，其余百分号编码",
                "空路径用 / 表示",
                "查询串单独规范化（按 key 排序），不并入路径",
                "客户端与服务器必须用同一套编码规则，否则中文/特殊字符路径签名对不上"
        });
        result.put("example", "/api/v1/users/张三 → 规范化 → /api/v1/users/%E5%BC%A0%E4%B8%89");
        result.put("tip", "Java 里用 java.net.URLEncoder 时注意它把空格编码成 +，HTTP 路径规范要 %20——两端规则必须一致。");
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("why", "签名比对的是字符串：任何一端拼串规则与另一端不同（顺序/大小写/编码），签名就失配。"
                + "规范化 = 给「如何拼串」定一套唯一规则，让两端必然一致");
        result.put("canonicalization", new String[]{
                "CanonicalURI：路径按 RFC 3986 编码、空路径用 /",
                "CanonicalQueryString：key 按字典序排序，key1=val1&key2=val2",
                "CanonicalHeaders：头名转小写按字典序排序，key:value 换行",
                "空字段（无 body/无 query/无参与头）一律空串"
        });
        result.put("commonBugs", new String[]{
                "query 忘了排序",
                "Header 大小写不一致",
                "URL 编码用错（+ vs %20）",
                "空字段规则不一致（null vs 空串）"
        });
        result.put("tip", "调试签名对不上的第一件事：把客户端和服务端的 canonicalString 打出来逐字比对，一眼就能看出差异。");
        return result;
    }
}
