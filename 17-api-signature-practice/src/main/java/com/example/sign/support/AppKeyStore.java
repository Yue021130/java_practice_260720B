package com.example.sign.support;

import com.example.sign.config.SignPracticeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * appid → appkey 注册表（内存版，模拟服务端密钥库）。
 *
 * 真实工程里 appkey 存数据库/配置中心，且做加密存储；这里为教学用内存 Map。
 * 核心语义：appid 可公开传输（定位用），appkey 只在服务端查出来算签名，绝不外发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppKeyStore {

    private final SignPracticeProperties props;

    /** appid → 应用信息（appkey + 备注） */
    private final Map<String, AppInfo> store = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register(props.getDemoAppId(), props.getDemoAppKey(), "演示应用（前端面板默认）");
        log.info("AppKeyStore 就绪：已注册 appid = {}", props.getDemoAppId());
    }

    /**
     * 注册一个应用。
     */
    public void register(String appId, String appKey, String note) {
        store.put(appId, new AppInfo(appKey, note));
    }

    /**
     * 根据 appid 查 appkey；不存在返回 null（调用方按「AppId 不存在」拒绝）。
     */
    public String getAppKey(String appId) {
        AppInfo info = store.get(appId);
        return info == null ? null : info.getAppKey();
    }

    public boolean exists(String appId) {
        return store.containsKey(appId);
    }

    public Map<String, String> list() {
        Map<String, String> result = new LinkedHashMap<>();
        store.forEach((id, info) -> result.put(id, info.getNote()));
        return result;
    }

    private static class AppInfo {
        private final String appKey;
        private final String note;

        AppInfo(String appKey, String note) {
            this.appKey = appKey;
            this.note = note;
        }

        String getAppKey() {
            return appKey;
        }

        String getNote() {
            return note;
        }
    }
}
