package com.edus.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SQL性能监控组件
 */
@Component
public class SqlPerformanceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(SqlPerformanceMonitor.class);

    // SQL执行统计信息
    private final ConcurrentHashMap<String, SqlStats> sqlStatsMap = new ConcurrentHashMap<>();

    /**
     * 记录SQL执行
     */
    public void recordSqlExecution(String sqlId, String sql, long executionTime, boolean success) {
        SqlStats stats = sqlStatsMap.computeIfAbsent(sqlId, k -> new SqlStats());

        stats.incrementTotalCount();
        stats.addExecutionTime(executionTime);

        if (success) {
            stats.incrementSuccessCount();
        } else {
            stats.incrementFailureCount();
        }

        if (executionTime > 1000) {
            stats.incrementSlowSqlCount();
        }

        // 定期打印统计信息
        if (stats.getTotalCount() % 100 == 0) {
            logStatistics();
        }
    }

    /**
     * 获取SQL统计信息
     */
    public SqlStats getSqlStats(String sqlId) {
        return sqlStatsMap.get(sqlId);
    }

    /**
     * 获取所有SQL统计信息
     */
    public ConcurrentHashMap<String, SqlStats> getAllSqlStats() {
        return sqlStatsMap;
    }

    /**
     * 打印统计信息
     */
    public void logStatistics() {
        logger.info("=== SQL性能统计报告 ===");

        sqlStatsMap.forEach((sqlId, stats) -> {
            if (stats.getTotalCount() > 0) {
                logger.info("SQL ID: {}", sqlId);
                logger.info("  总执行次数: {}", stats.getTotalCount());
                logger.info("  成功次数: {}", stats.getSuccessCount());
                logger.info("  失败次数: {}", stats.getFailureCount());
                logger.info("  慢SQL次数: {}", stats.getSlowSqlCount());
                logger.info("  平均执行时间: {}ms", stats.getAverageExecutionTime());
                logger.info("  最大执行时间: {}ms", stats.getMaxExecutionTime());
                logger.info("  最小执行时间: {}ms", stats.getMinExecutionTime());
            }
        });

        logger.info("=== 统计报告结束 ===");
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        sqlStatsMap.clear();
        logger.info("SQL统计信息已重置");
    }

    /**
     * SQL统计信息类
     */
    public static class SqlStats {
        private final AtomicLong totalCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong slowSqlCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private volatile long maxExecutionTime = 0;
        private volatile long minExecutionTime = Long.MAX_VALUE;

        public void incrementTotalCount() {
            totalCount.incrementAndGet();
        }

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }

        public void incrementSlowSqlCount() {
            slowSqlCount.incrementAndGet();
        }

        public void addExecutionTime(long executionTime) {
            totalExecutionTime.addAndGet(executionTime);
            if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            }
        }

        public long getTotalCount() {
            return totalCount.get();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public long getSlowSqlCount() {
            return slowSqlCount.get();
        }

        public double getAverageExecutionTime() {
            long count = totalCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0;
        }

        public long getMaxExecutionTime() {
            return maxExecutionTime;
        }

        public long getMinExecutionTime() {
            return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime;
        }
    }
}