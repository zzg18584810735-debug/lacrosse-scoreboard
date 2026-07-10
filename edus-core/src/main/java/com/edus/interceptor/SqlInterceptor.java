package com.edus.interceptor;

import com.edus.monitor.SqlPerformanceMonitor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * SQL执行拦截器 - 用于监控SQL执行情况
 * 支持慢SQL检测、性能统计和异常监控
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(SqlInterceptor.class);

    @Autowired
    private SqlPerformanceMonitor sqlPerformanceMonitor;

    // 慢SQL阈值（毫秒） - 可通过配置文件配置
    @Value("${edus.performance.monitoring.slow-sql-threshold:1000}")
    private long slowSqlThreshold;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;

        String sqlId = mappedStatement.getId();
        long startTime = System.currentTimeMillis();

        try {
            Object result = invocation.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();

            // 记录SQL执行信息
            logSqlExecution(sqlId, sql, parameter, executionTime, true);

            // 记录到性能监控组件
            if (sqlPerformanceMonitor != null) {
                sqlPerformanceMonitor.recordSqlExecution(sqlId, sql, executionTime, true);
            }

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();

            logger.error("SQL执行异常 - ID: {}, SQL: {}, 参数: {}, 耗时: {}ms, 异常: {}",
                sqlId, sql, parameter, executionTime, e.getMessage());

            // 记录失败到性能监控组件
            if (sqlPerformanceMonitor != null) {
                sqlPerformanceMonitor.recordSqlExecution(sqlId, sql, executionTime, false);
            }

            throw e;
        }
    }

    private void logSqlExecution(String sqlId, String sql, Object parameter, long executionTime, boolean success) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("SQL执行记录 - ");
        logMessage.append("ID: ").append(sqlId).append(", ");
        logMessage.append("SQL: ").append(sql).append(", ");
        logMessage.append("参数: ").append(parameter != null ? parameter.toString() : "null").append(", ");
        logMessage.append("耗时: ").append(executionTime).append("ms, ");
        logMessage.append("状态: ").append(success ? "成功" : "失败");

        if (executionTime > slowSqlThreshold) {
            logger.warn("⚠️ 慢SQL检测 - {}", logMessage);
        } else {
            logger.debug("✅ {}", logMessage);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以通过属性配置拦截器
        String threshold = properties.getProperty("slowSqlThreshold");
        if (threshold != null) {
            this.slowSqlThreshold = Long.parseLong(threshold);
        }
    }

    public void setSlowSqlThreshold(long slowSqlThreshold) {
        this.slowSqlThreshold = slowSqlThreshold;
    }
}
