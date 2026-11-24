package com.example.tlp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 场景演示用的业务方法。
 *
 * <p>@Async 方法必须放在独立的 Spring Bean 里：在 Controller 中
 * this 自调用不走 AOP 代理，注解会静默失效——这是 @Async 最常见
 * 的坑，本类独立出来就是为了避开它。</p>
 */
@Slf4j
@Service
public class DemoService {

    /** 场景 2：@Async 异步线程（traceId 靠 MdcTaskDecorator 透传） */
    @Async("batchExecutor")
    public void asyncWork() {
        log.info("@Async 异步任务执行（线程名应以 batch- 开头）");
    }
}
