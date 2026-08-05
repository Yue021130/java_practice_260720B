package com.example.exception.concurrency;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步任务服务。
 */
@Service
public class AsyncTaskService {

    @Async
    public void asyncWithException() {
        throw new RuntimeException("@Async 方法内部异常");
    }
}
