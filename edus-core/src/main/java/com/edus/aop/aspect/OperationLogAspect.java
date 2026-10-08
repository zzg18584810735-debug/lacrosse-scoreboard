package com.edus.aop.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.edus.aop.annotation.LogOperation;
import com.edus.common.utils.Result;
import com.edus.common.utils.UserContext;
import com.edus.common.utils.WebUtils;
import com.edus.entity.OperationLog;
import com.edus.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * 操作日志切面
 * 拦截带有@LogOperation注解的方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Pointcut("@annotation(com.edus.aop.annotation.LogOperation)")
    public void operationLogPointcut() {}

    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();
        Object result;
        ServletRequestAttributes attributes;
        HttpServletRequest request;
        LogOperation annotation = null;

        try {
            // 获取请求信息
            attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();

                // 获取注解信息
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                annotation = signature.getMethod().getAnnotation(LogOperation.class);

                // 设置基本信息
                operationLog.setOperationType(annotation.operationType());
                operationLog.setModule(annotation.module());
                operationLog.setDescription(annotation.description());
                operationLog.setRequestMethod(request.getMethod());
                operationLog.setRequestUrl(request.getRequestURI());
                operationLog.setIpAddress(WebUtils.getClientIP(request));
                operationLog.setUserAgent(request.getHeader("User-Agent"));

                // 设置用户信息
                operationLog.setUserId(UserContext.getCurrentUserId());
                operationLog.setUsername(UserContext.getCurrentUserName());
                fillLoginAttemptUsername(operationLog, annotation, joinPoint.getArgs());

                // 记录请求参数
                if (annotation.logParams()) {
                    Object[] args = joinPoint.getArgs();
                    if (args.length > 0) {
                        try {
                            String params = JSONUtil.toJsonStr(args);
                            operationLog.setRequestParams(StrUtil.sub(params, 0, 2000));
                        } catch (Exception e) {
                            operationLog.setRequestParams(Arrays.toString(args));
                        }
                    }
                }
            }

            // 执行方法
            result = joinPoint.proceed();

            // 记录响应结果
            if (annotation != null && annotation.logResponse() && result != null) {
                try {
                    String response = JSONUtil.toJsonStr(result);
                    operationLog.setResponseResult(StrUtil.sub(response, 0, 2000));
                } catch (Exception e) {
                    operationLog.setResponseResult(result.toString());
                }
            }

            fillLoginUserInfo(operationLog, annotation, result);

            operationLog.setStatus(1);
            return result;

        } catch (Exception e) {
            operationLog.setStatus(0);
            operationLog.setErrorMessage(StrUtil.sub(e.getMessage(), 0, 1000));
            throw e;
        } finally {
            // 设置执行时长
            operationLog.setExecuteTime(System.currentTimeMillis() - startTime);
            operationLog.setCreatedAt(LocalDateTime.now());

            // 异步保存日志
            try {
                operationLogService.saveAsync(operationLog);
            } catch (Exception e) {
                log.error("操作日志保存失败", e);
            }
        }
    }

    /**
     * 登录接口执行前通常没有UserContext，登录成功后从响应data中补充用户信息。
     */
    private void fillLoginUserInfo(OperationLog operationLog, LogOperation annotation, Object result) {
        if (annotation == null || !"LOGIN".equalsIgnoreCase(annotation.operationType())) {
            return;
        }
        if (operationLog.getUserId() != null && StrUtil.isNotBlank(operationLog.getUsername())) {
            return;
        }
        if (!(result instanceof Result<?> response) || response.getCode() == null || response.getCode() != 200) {
            return;
        }
        if (!(response.getData() instanceof Map<?, ?> data)) {
            return;
        }

        Object userId = data.get("userId");
        Object username = data.get("username");

        if (operationLog.getUserId() == null && userId != null) {
            try {
                operationLog.setUserId(Long.parseLong(String.valueOf(userId)));
            } catch (NumberFormatException e) {
                log.debug("登录日志userId转换失败：{}", userId);
            }
        }
        if (StrUtil.isBlank(operationLog.getUsername()) && username != null) {
            operationLog.setUsername(String.valueOf(username));
        }
    }

    /**
     * 登录失败时也记录尝试登录的账号，但不记录密码。
     */
    private void fillLoginAttemptUsername(OperationLog operationLog, LogOperation annotation, Object[] args) {
        if (annotation == null || !"LOGIN".equalsIgnoreCase(annotation.operationType())) {
            return;
        }
        if (StrUtil.isNotBlank(operationLog.getUsername()) || args == null) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof Map<?, ?> data) {
                Object username = data.get("username");
                if (username != null && StrUtil.isNotBlank(String.valueOf(username))) {
                    operationLog.setUsername(String.valueOf(username));
                    return;
                }
            }
        }
    }
}
