package com.edus.controller.example;

import com.edus.common.utils.Result;
import com.edus.entity.OperationLog;
import com.edus.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志示例Controller
 * 演示如何使用@OperationLog注解
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 查询操作日志列表
     * 使用@OperationLog注解记录查询操作
     */
    @GetMapping
    public Result<List<OperationLog>> getOperationLogs() {
        List<OperationLog> logs = operationLogService.list();
        return Result.ok(logs);
    }

    /**
     * 根据用户ID查询操作日志
     * 使用@OperationLog注解记录查询操作
     */
    @GetMapping("/user/{userId}")
    public Result<List<OperationLog>> getLogsByUserId(@PathVariable Long userId) {
        // 这里只是示例，实际应该使用条件查询
        List<OperationLog> logs = operationLogService.lambdaQuery()
            .eq(OperationLog::getUserId, userId)
            .list();
        return Result.ok(logs);
    }
}