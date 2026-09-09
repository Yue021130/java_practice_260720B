package com.example.tip.util;

import com.example.tip.dto.IpRegionVO;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ip2region IP 归属地查询工具
 *
 * <p>八股：ip2region 是什么？
 * 1. 离线 IP 数据管理框架，数据文件仅几 MB；
 * 2. 支持多种查询算法：memory、vectorIndex、cache；
 * 3. 返回格式：国家|区域|省份|城市|ISP。
 * </p>
 *
 * <p>本工具优先加载 classpath 下的 ip2region.xdb，若未找到则降级使用内置的常用 IP 段数据，
 * 保证示例可运行、功能可闭环。</p>
 */
@Slf4j
@Component
public class Ip2RegionUtil {

    private Searcher searcher;

    /** 内置 fallback 数据：IP 段 + region 字符串 */
    private final List<IpSegment> fallbackSegments = new ArrayList<>();

    /**
     * 启动时加载 ip2region.xdb，若不存在则初始化内置 fallback
     */
    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("ip2region.xdb");
        if (resource.exists()) {
            byte[] cBuff = resource.getInputStream().readAllBytes();
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("ip2region 加载完成，数据大小：{} bytes", cBuff.length);
        } else {
            log.warn("classpath 下未找到 ip2region.xdb，启用内置 IP 段 fallback");
            initFallbackSegments();
        }
    }

    private void initFallbackSegments() {
        // 常见公网 IP 段（startIp|endIp|region）
        addSegment("127.0.0.0", "127.255.255.255", "0|0|0|本地回环|0");
        addSegment("10.0.0.0", "10.255.255.255", "0|0|0|局域网|0");
        addSegment("172.16.0.0", "172.31.255.255", "0|0|0|局域网|0");
        addSegment("192.168.0.0", "192.168.255.255", "0|0|0|局域网|0");
        addSegment("8.8.8.0", "8.8.8.255", "美国|0|0|加利福尼亚|Google DNS");
        addSegment("1.1.1.0", "1.1.1.255", "美国|0|0|加利福尼亚|Cloudflare");
        addSegment("114.114.114.0", "114.114.114.255", "中国|0|江苏省|南京市|电信");
        addSegment("223.5.5.0", "223.5.5.255", "中国|0|浙江省|杭州市|阿里巴巴");
        addSegment("220.181.38.0", "220.181.38.255", "中国|0|北京市|北京市|电信");
        addSegment("119.29.29.0", "119.29.29.255", "中国|0|广东省|深圳市|腾讯");
        addSegment("180.76.76.0", "180.76.76.255", "中国|0|北京市|北京市|百度");
        addSegment("0.0.0.0", "255.255.255.255", "0|0|0|未知|0");
        fallbackSegments.sort(Comparator.comparingLong(IpSegment::getStart));
    }

    private void addSegment(String startIp, String endIp, String region) {
        try {
            fallbackSegments.add(new IpSegment(ip2long(startIp), ip2long(endIp), region));
        } catch (Exception e) {
            log.warn("内置 IP 段解析失败 {}-{}: {}", startIp, endIp, e.getMessage());
        }
    }

    /**
     * 查询 IP 归属地
     */
    public IpRegionVO search(String ip) {
        String region;
        if (searcher != null) {
            try {
                long sTime = System.nanoTime();
                region = searcher.search(ip);
                long cost = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - sTime);
                log.info("IP查询 ip={} region={} cost={} μs", ip, region, cost);
            } catch (Exception e) {
                log.warn("ip2region 查询失败 ip={}", ip, e);
                region = fallbackSearch(ip);
            }
        } else {
            region = fallbackSearch(ip);
        }
        return parseRegion(ip, region);
    }

    private String fallbackSearch(String ip) {
        try {
            long ipLong = ip2long(ip);
            // 二分查找包含该 IP 的段
            int left = 0, right = fallbackSegments.size() - 1;
            while (left <= right) {
                int mid = (left + right) >>> 1;
                IpSegment seg = fallbackSegments.get(mid);
                if (ipLong < seg.getStart()) {
                    right = mid - 1;
                } else if (ipLong > seg.getEnd()) {
                    left = mid + 1;
                } else {
                    return seg.getRegion();
                }
            }
        } catch (Exception e) {
            log.warn("fallback IP 查询失败 ip={}", ip, e);
        }
        return "0|0|0|未知|0";
    }

    private IpRegionVO parseRegion(String ip, String region) {
        List<String> parts = Arrays.asList(region.split("\\|"));
        String country = getPart(parts, 0);
        String province = getPart(parts, 2);
        String city = getPart(parts, 3);
        String isp = getPart(parts, 4);
        String full = buildFullAddress(country, province, city, isp);
        return new IpRegionVO(ip, country, province, city, isp, full);
    }

    private String getPart(List<String> parts, int index) {
        if (index < parts.size()) {
            String v = parts.get(index);
            return (v == null || "0".equals(v) || v.isEmpty()) ? "" : v;
        }
        return "";
    }

    private String buildFullAddress(String country, String province, String city, String isp) {
        StringBuilder sb = new StringBuilder();
        if (!country.isEmpty()) sb.append(country);
        if (!province.isEmpty()) sb.append(province);
        if (!city.isEmpty()) sb.append(city);
        if (!isp.isEmpty()) sb.append(" ").append(isp);
        return sb.toString().trim();
    }

    /**
     * IPv4 转 long
     */
    private long ip2long(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (String part : parts) {
            result = (result << 8) | Integer.parseInt(part);
        }
        return result;
    }

    @PreDestroy
    public void destroy() throws IOException {
        if (searcher != null) {
            searcher.close();
        }
    }

    private static class IpSegment {
        private final long start;
        private final long end;
        private final String region;

        IpSegment(long start, long end, String region) {
            this.start = start;
            this.end = end;
            this.region = region;
        }

        long getStart() {
            return start;
        }

        long getEnd() {
            return end;
        }

        String getRegion() {
            return region;
        }
    }
}
