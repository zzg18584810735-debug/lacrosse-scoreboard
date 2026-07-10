package com.edus.common.components;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 异步任务提交工具，支持业务级重试和超时控制。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskManager {

    private final ThreadPoolTaskExecutor taskExecutor;

    public CompletableFuture<Void> runAsyncWithRetry(String taskName,
                                                     Runnable task,
                                                     int maxAttempts,
                                                     long timeoutSeconds) {
        return supplyAsyncWithRetry(taskName, () -> {
            task.run();
            return null;
        }, maxAttempts, timeoutSeconds);
    }

    public <T> CompletableFuture<T> supplyAsyncWithRetry(String taskName,
                                                         Supplier<T> task,
                                                         int maxAttempts,
                                                         long timeoutSeconds) {
        int attempts = Math.max(maxAttempts, 1);
        CompletableFuture<T> future = CompletableFuture.supplyAsync(
                () -> executeWithRetry(taskName, task, attempts),
                taskExecutor
        );

        if (timeoutSeconds > 0) {
            return future.orTimeout(timeoutSeconds, TimeUnit.SECONDS);
        }

        return future;
    }

    private <T> T executeWithRetry(String taskName, Supplier<T> task, int maxAttempts) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return task.get();
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("异步任务执行失败，准备重试：taskName={}, attempt={}/{}",
                        taskName, attempt, maxAttempts, e);
            }
        }

        throw lastException;
    }
}
