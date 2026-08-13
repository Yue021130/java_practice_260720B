package com.example.sign.nonce;

import com.example.sign.support.NonceStore;
import com.example.sign.support.SignLogStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 05. 防重放-nonce：重复使用拒绝。
 *
 * nonce 是「一次性随机串」，服务端必须记录用过的 nonce（Redis SETNX + TTL）。
 * 同一请求被窗口内重放时，nonce 已存在 → 拒绝。教学用 NonceStore 内存模拟。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonceService {

    private final NonceStore nonceStore;
    private final SignLogStore logStore;

    /**
     * nonce 去重演示：传一个 nonce，第一次 tryAcquire 成功，第二次必然失败。
     * 为了直观，接口一次调用内演示两次占用同一 nonce。
     */
    public Map<String, Object> demo(String nonce) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean first = nonceStore.tryAcquire(nonce);
        boolean second = nonceStore.tryAcquire(nonce);

        result.put("nonce", nonce);
        result.put("firstAcquire", first);
        result.put("secondAcquire", second);
        result.put("storedCount", nonceStore.size());
        result.put("reason", first && !second ? "第一次占用成功，第二次（重放）被拒绝：nonce 已存在"
                : first ? "第一次占用成功"
                : "该 nonce 已被使用过（可能来自之前的演示）");
        result.put("tip", "nonce 必须由客户端每次请求随机生成（如 UUID），且每个 nonce 只用一次："
                + "服务端记下已用的，窗口内重放立刻露馅。");

        logStore.add("nonce", "demo", first && !second, "nonce=" + nonce);
        return result;
    }

    public Map<String, Object> explain() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("what", "nonce（Number used ONCE）= 一次性随机数，客户端每次请求生成一个，随请求一起发送并参与签名");
        result.put("redis", "真实工程：Redis SET nonce:{nonce} 1 EX 300 NX —— 原子「不存在才写入 + TTL 过期自动删」。"
                + "NX 保证并发下只有一个请求能占用成功");
        result.put("whyMemorize", "nonce 必须服务端记忆才能判断「是否用过」：无状态方案做不到防重放，"
                + "这也是 HMAC 方案里唯一需要存储的地方");
        result.put("note", "nonce 要足够随机（UUID/安全随机数），否则可被预测；TTL 与时间戳窗口一致，避免存储无限膨胀");
        result.put("tip", "时间戳挡「老请求」，nonce 挡「窗口内重放」：一个 cheap 一个要存储，组合才是完整防重放。");
        return result;
    }
}
