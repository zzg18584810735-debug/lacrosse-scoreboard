package com.edus.controller;

import com.edus.common.utils.Result;
import com.edus.monitor.AsyncTaskMonitor;
import com.edus.monitor.SqlPerformanceMonitor;
import com.edus.security.monitor.MonitorAccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final SqlPerformanceMonitor sqlPerformanceMonitor;
    private final AsyncTaskMonitor asyncTaskMonitor;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final MonitorAccessGuard monitorAccessGuard;

    /**
     * 获取所有SQL统计信息
     */
    @GetMapping("/sql-stats")
    public Result<Map<String, Object>> getSqlStats(HttpServletRequest request) {
        monitorAccessGuard.requireReadAccess(request);

        Map<String, Object> result = new HashMap<>();
        result.put("sqlStats", sqlPerformanceMonitor.getAllSqlStats());
        result.put("timestamp", System.currentTimeMillis());
        return Result.ok(result);
    }

    /**
     * 获取特定SQL的统计信息
     */
    @GetMapping("/sql-stats/{sqlId}")
    public Result<SqlPerformanceMonitor.SqlStats> getSqlStatsById(@PathVariable String sqlId,
                                                                  HttpServletRequest request) {
        monitorAccessGuard.requireReadAccess(request);

        SqlPerformanceMonitor.SqlStats stats = sqlPerformanceMonitor.getSqlStats(sqlId);
        if (stats == null) {
            return Result.error("未找到指定的SQL统计信息");
        }
        return Result.ok(stats);
    }

    /**
     * 打印统计报告到日志
     */
    @PostMapping("/sql-stats/print")
    public Result<Map<String, String>> printStatistics(HttpServletRequest request) {
        monitorAccessGuard.requireWriteAccess(request);

        sqlPerformanceMonitor.logStatistics();
        Map<String, String> result = new HashMap<>();
        result.put("message", "SQL统计报告已打印到日志");
        return Result.ok(result);
    }

    /**
     * 重置统计信息
     */
    @PostMapping("/sql-stats/reset")
    public Result<Map<String, String>> resetStatistics(HttpServletRequest request) {
        monitorAccessGuard.requireWriteAccess(request);

        sqlPerformanceMonitor.resetStatistics();
        Map<String, String> result = new HashMap<>();
        result.put("message", "SQL统计信息已重置");
        return Result.ok(result);
    }

    /**
     * 获取异步线程池状态
     */
    @GetMapping("/async")
    public Result<Map<String, Object>> getAsyncStats(HttpServletRequest request) {
        monitorAccessGuard.requireReadAccess(request);
        return Result.ok(asyncTaskMonitor.getSnapshot(taskExecutor));
    }

    /**
     * 获取系统监控概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(HttpServletRequest request) {
        monitorAccessGuard.requireReadAccess(request);

        Map<String, Object> overview = new HashMap<>();

        // 计算总体统计
        var allStats = sqlPerformanceMonitor.getAllSqlStats();
        long totalQueries = 0;
        long totalSlowQueries = 0;
        long totalFailures = 0;
        double totalAvgTime = 0;

        for (var entry : allStats.entrySet()) {
            SqlPerformanceMonitor.SqlStats stats = entry.getValue();
            totalQueries += stats.getTotalCount();
            totalSlowQueries += stats.getSlowSqlCount();
            totalFailures += stats.getFailureCount();
            totalAvgTime += stats.getAverageExecutionTime();
        }

        overview.put("totalQueries", totalQueries);
        overview.put("totalSlowQueries", totalSlowQueries);
        overview.put("totalFailures", totalFailures);
        overview.put("averageExecutionTime", allStats.isEmpty() ? 0 : totalAvgTime / allStats.size());
        overview.put("uniqueSqlStatements", allStats.size());
        overview.put("async", asyncTaskMonitor.getSnapshot(taskExecutor));
        overview.put("timestamp", System.currentTimeMillis());

        return Result.ok(overview);
    }
}
