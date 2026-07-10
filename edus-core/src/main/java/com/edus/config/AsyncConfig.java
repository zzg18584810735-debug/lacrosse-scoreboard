package com.edus.config;

import com.edus.monitor.AsyncTaskMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置类
 * 用于配置@Async注解的线程池
 * 适用于所有异步任务，包括操作日志、邮件发送、文件处理等
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncTaskMonitor asyncTaskMonitor;

    @Value("${edus.async.timeout-warning-millis:30000}")
    private long timeoutWarningMillis;

    public AsyncConfig(AsyncTaskMonitor asyncTaskMonitor) {
        this.asyncTaskMonitor = asyncTaskMonitor;
    }

    /**
     * 默认异步任务线程池
     * 核心线程数：5
     * 最大线程数：10
     * 队列容量：100
     */
    @Bean(name = "taskExecutor")
    @Override
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀（通用名称，适用于所有异步任务）
        executor.setThreadNamePrefix("async-task-");

        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 记录任务执行状态、耗时和超时告警
        executor.setTaskDecorator(asyncTaskDecorator());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("异步任务线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> log.error("异步任务执行异常 - 方法: {}, 参数: {}, 异常: {}",
                method.getName(), params, throwable.getMessage(), throwable);
    }

    private TaskDecorator asyncTaskDecorator() {
        return runnable -> () -> {
            long startTime = System.currentTimeMillis();
            asyncTaskMonitor.recordStart();
            try {
                runnable.run();
                long executionTime = System.currentTimeMillis() - startTime;
                asyncTaskMonitor.recordSuccess(executionTime, timeoutWarningMillis);
                if (timeoutWarningMillis > 0 && executionTime > timeoutWarningMillis) {
                    log.warn("异步任务执行超时告警：thread={}, executionTime={}ms, threshold={}ms",
                            Thread.currentThread().getName(), executionTime, timeoutWarningMillis);
                }
            } catch (Throwable throwable) {
                long executionTime = System.currentTimeMillis() - startTime;
                asyncTaskMonitor.recordFailure(executionTime, timeoutWarningMillis);
                throw throwable;
            } finally {
                asyncTaskMonitor.recordFinish();
            }
        };
    }
}
