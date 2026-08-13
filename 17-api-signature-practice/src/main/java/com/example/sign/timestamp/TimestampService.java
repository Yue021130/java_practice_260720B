package com.example.sign.timestamp;

import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 04. 防重放-时间戳：允许 ±skew 秒的偏差。
 *
 * 时间戳能挡住「过期很久的旧请求被重放」（如截获的昨天请求），
 * 因为时间戳超出窗口直接被拒；它挡不住「窗口内立刻重放」，
 * 那部分交给 nonce（第 05 章）。两者缺一不可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimestampService {

    private final SignLogStore logStore;

    /**
     * 时间戳窗口校验演示。
     *
     * @param timestamp  待校验时间戳；特殊值 now / -3600 / +3600 便于演示
     * @param skewSeconds 允许窗口（秒）
     */
    public Map<String, Object> demo(String timestamp, long skewSeconds) {
        long safeSkew = Math.max(1, skewSeconds);
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> result = new LinkedHashMap<>();

        long ts;
        switch (timestamp) {
            case "now":
                ts = now;
                break;
            case "-3600":
                ts = now - 3600;      // 一小时前 → 过期
                break;
            case "+3600":
                ts = now + 3600;      // 一小时后 → 未来（也拒绝）
                break;
            default:
                try {
                    ts = Long.parseLong(timestamp);
                } catch (NumberFormatException e) {
                    ts = now;
                }
        }
        long diff = now - ts;
        boolean passed = Math.abs(diff) <= safeSkew;

        result.put("now", now);
        result.put("clientTimestamp", ts);
        result.put("skewSeconds", safeSkew);
        result.put("diffSeconds", diff);
        result.put("passed", passed);
        result.put("reason", !passed ? (diff > 0 ? "请求已过期（晚了 " + diff + " 秒）" : "请求来自未来（早了 " + -diff + " 秒）")
                : "在窗口内，通过");
        result.put("tip", "允许 ±" + safeSkew + " 秒偏差：过期请求（旧重放）与未来请求（时钟漂移）都会被拒，"
                + "窗口内的瞬间重放由 nonce 兜底。");

        logStore.add("timestamp", "demo", passed, "时间戳偏差 " + diff + " 秒");
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("why", new String[]{
                "重放的旧请求：截获一次请求，之后无限重放，若没有时间校验就无法识别",
                "时间戳让「过期请求」直接失效：超出 ±窗口即拒绝，成本极低",
                "为什么是 ±：客户端时钟可能不准（快了/慢了），留一点容差；同时防未来时间戳绕过"
        });
        result.put("window", "默认 ±5 分钟（300s）：窗口太大，重放窗口大；太小，时钟漂移误伤。生产按业务时延敏感度调");
        result.put("vsNonce", "时间戳 = 粗粒度防重放（挡「很久以前」的请求）；nonce = 细粒度防重放（挡「窗口内第二次」）。"
                + "只靠时间戳，5 分钟窗口内可无限重放；只靠 nonce 没有时间上限，存储会无限膨胀——两者配合");
        result.put("clockSkew", "注意服务器时间要 NTP 同步，否则会误判所有请求过期");
        result.put("tip", "面试：时间戳 + nonce 是防重放的黄金组合，各管一段，缺一不可。");
        return result;
    }
}
