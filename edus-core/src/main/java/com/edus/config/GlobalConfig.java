package com.edus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局配置类
 * 包含跨域配置、拦截器、过滤器等
 */
@Configuration
public class GlobalConfig implements WebMvcConfigurer {

    /**
     * 配置跨域请求
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的源（开发环境可以使用*，生产环境建议指定具体域名）
                .allowedOriginPatterns("*")
                // 允许的HTTP方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                // 允许的请求头
                .allowedHeaders("*")
                // 允许发送Cookie信息
                .allowCredentials(true)
                // 预检请求的有效期，单位秒（1小时内不需要再发送预检请求）
                .maxAge(3600);
    }
}