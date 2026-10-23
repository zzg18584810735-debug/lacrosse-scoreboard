package com.edus.example.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edus.example.entity.OperationLog;
import com.edus.example.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {

    /**
     * 异步保存操作日志
     * 使用默认异步线程池执行
     */
    @Async
    public void saveAsync(OperationLog operationLog) {
        try {
            this.save(operationLog);
            log.debug("操作日志保存成功: {}", operationLog.getDescription());
        } catch (Exception e) {
            log.error("操作日志保存失败: {}", operationLog.getDescription(), e);
        }
    }
}
