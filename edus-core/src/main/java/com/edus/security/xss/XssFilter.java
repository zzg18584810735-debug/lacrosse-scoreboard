package com.edus.security.xss;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * XSS防护过滤器
 * 拦截所有HTTP请求，对请求参数进行XSS过滤
 *
 * @author edus
 */
@Slf4j
@Component
@Order(1) // 确保优先级最高，在其他过滤器之前执行
public class XssFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XSS防护过滤器已初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 排除文件上传请求，避免影响文件上传
        String contentType = httpRequest.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            chain.doFilter(request, response);
            return;
        }

        // 包装请求，启用XSS过滤
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);

        chain.doFilter(xssRequest, response);
    }

    @Override
    public void destroy() {
        log.info("XSS防护过滤器已销毁");
    }
}