package com.edus.monitor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步任务监控组件。
 */
@Component
public class AsyncTaskMonitor {

    private final AtomicLong startedCount = new AtomicLong();
    private final AtomicLong completedCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicLong timeoutWarningCount = new AtomicLong();
    private final AtomicLong runningCount = new AtomicLong();
    private final AtomicLong totalExecutionTimeMillis = new AtomicLong();
    private final AtomicLong maxExecutionTimeMillis = new AtomicLong();

    public void recordStart() {
        startedCount.incrementAndGet();
        runningCount.incrementAndGet();
    }

    public void recordSuccess(long executionTimeMillis, long timeoutWarningMillis) {
        completedCount.incrementAndGet();
        recordExecutionTime(executionTimeMillis, timeoutWarningMillis);
    }

    public void recordFailure(long executionTimeMillis, long timeoutWarningMillis) {
        failedCount.incrementAndGet();
        recordExecutionTime(executionTimeMillis, timeoutWarningMillis);
    }

    public void recordFinish() {
        runningCount.decrementAndGet();
    }

    public Map<String, Object> getSnapshot(ThreadPoolTaskExecutor executor) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

        long finishedCount = completedCount.get() + failedCount.get();
        snapshot.put("corePoolSize", executor.getCorePoolSize());
        snapshot.put("maxPoolSize", executor.getMaxPoolSize());
        snapshot.put("poolSize", threadPoolExecutor.getPoolSize());
        snapshot.put("activeCount", threadPoolExecutor.getActiveCount());
        snapshot.put("queueSize", threadPoolExecutor.getQueue().size());
        snapshot.put("queueRemainingCapacity", threadPoolExecutor.getQueue().remainingCapacity());
        snapshot.put("largestPoolSize", threadPoolExecutor.getLargestPoolSize());
        snapshot.put("threadPoolCompletedTaskCount", threadPoolExecutor.getCompletedTaskCount());
        snapshot.put("startedCount", startedCount.get());
        snapshot.put("runningCount", runningCount.get());
        snapshot.put("completedCount", completedCount.get());
        snapshot.put("failedCount", failedCount.get());
        snapshot.put("timeoutWarningCount", timeoutWarningCount.get());
        snapshot.put("averageExecutionTimeMillis",
                finishedCount == 0 ? 0 : totalExecutionTimeMillis.get() / finishedCount);
        snapshot.put("maxExecutionTimeMillis", maxExecutionTimeMillis.get());
        snapshot.put("timestamp", System.currentTimeMillis());
        return snapshot;
    }

    private void recordExecutionTime(long executionTimeMillis, long timeoutWarningMillis) {
        totalExecutionTimeMillis.addAndGet(executionTimeMillis);
        maxExecutionTimeMillis.accumulateAndGet(executionTimeMillis, Math::max);
        if (timeoutWarningMillis > 0 && executionTimeMillis > timeoutWarningMillis) {
            timeoutWarningCount.incrementAndGet();
        }
    }
}
