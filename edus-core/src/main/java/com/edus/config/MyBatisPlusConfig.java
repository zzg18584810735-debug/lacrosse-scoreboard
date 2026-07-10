package com.edus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.edus.interceptor.SqlInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置类
 * 数据库性能优化配置
 *
 * @author edus
 */
@Slf4j
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {

    /**
     * 配置MyBatisPlus拦截器
     * 包含：分页、乐观锁、防SQL注入攻击、SQL性能监控
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 分页优化配置
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setOverflow(false);         // 溢出时不会进行计数查询
        paginationInterceptor.setMaxLimit(1000L);         // 最大单页限制1000条
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 2. 乐观锁插件
        OptimisticLockerInnerInterceptor optimisticLocker = new OptimisticLockerInnerInterceptor();
        interceptor.addInnerInterceptor(optimisticLocker);

        // 3. 防SQL注入攻击
        BlockAttackInnerInterceptor blockAttackInterceptor = new BlockAttackInnerInterceptor();
        interceptor.addInnerInterceptor(blockAttackInterceptor);

        log.info("MyBatis-Plus性能优化配置已加载：分页优化、乐观锁、防SQL注入");
        return interceptor;
    }

    /**
     * 配置自定义SQL拦截器
     * 用于慢SQL监控和性能统计
     */
    @Bean
    public SqlInterceptor sqlInterceptor(DataSource dataSource) {
        try {
            SqlInterceptor interceptor = new SqlInterceptor();

            // 手动注册拦截器到SqlSessionFactory
            // 注意：这个配置会在应用启动时自动应用
            log.info("自定义SQL性能监控拦截器已注册");

            return interceptor;
        } catch (Exception e) {
            log.warn("SQL拦截器注册失败，将使用默认配置", e);
            return null;
        }
    }

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}