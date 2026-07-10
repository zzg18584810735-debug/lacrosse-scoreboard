package com.edus.common.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Web工具类
 * 提供Web开发中的常用工具方法，结合hutool实现
 *
 * @author edus
 */
@Slf4j
public class WebUtils {

    /**
     * 获取客户端IP地址
     * 支持通过代理服务器获取真实IP
     *
     * @return 客户端IP地址，获取失败返回"unknown"
     */
    public static String getClientIP() {
        HttpServletRequest request = getRequest();
        return getClientIP(request);
    }

    /**
     * 获取客户端IP地址（兼容方法）
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    public static String getClientIP(HttpServletRequest request) {
        return getClientIpAddr(request);
    }

    /**
     * 获取客户端真实IP地址
     * 优先从代理头中获取，如果没有则使用 remoteAddr
     * 支持多级代理场景，处理本地回环地址
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址，如果是本地访问则返回"127.0.0.1"
     */
    public static String getClientIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = null;
        // 按优先级检查各种代理头
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String value = request.getHeader(header);
            if (StrUtil.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
                // 如果有多个IP（通常用逗号分隔），取第一个
                int index = value.indexOf(',');
                ip = index > 0 ? value.substring(0, index).trim() : value.trim();
                break;
            }
        }

        // 如果从代理头中没有获取到，使用remoteAddr
        if (StrUtil.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理本地回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 判断是否为本地地址
     *
     * @param ip IP地址
     * @return 是否为本地地址
     */
    public static boolean isLocalAddress(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return "127.0.0.1".equals(ip) ||
                "0:0:0:0:0:0:0:1".equals(ip) ||
                "::1".equals(ip) ||
                "localhost".equalsIgnoreCase(ip);
    }

    /**
     * 获取当前HTTP请求
     *
     * @return 当前请求，获取失败返回null
     */
    public static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.warn("获取HttpServletRequest失败", e);
            return null;
        }
    }

    /**
     * 判断请求是否为AJAX请求
     *
     * @param request HTTP请求
     * @return 是否为AJAX请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        try {
            String accept = request.getHeader("Accept");
            String xRequestedWith = request.getHeader("X-Requested-With");

            return (accept != null && accept.contains("application/json")) ||
                    (xRequestedWith != null && xRequestedWith.contains("XMLHttpRequest"));
        } catch (Exception e) {
            log.debug("判断AJAX请求失败", e);
            return false;
        }
    }

    /**
     * 判断当前请求是否为AJAX请求
     *
     * @return 是否为AJAX请求
     */
    public static boolean isAjaxRequest() {
        HttpServletRequest request = getRequest();
        return isAjaxRequest(request);
    }

    /**
     * 获取请求的User-Agent信息
     *
     * @param request HTTP请求对象
     * @return User-Agent字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        return StrUtil.blankToDefault(userAgent, "unknown");
    }

    /**
     * 获取请求的User-Agent信息
     *
     * @return User-Agent字符串
     */
    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        return getUserAgent(request);
    }

    /**
     * 从User-Agent中提取浏览器信息
     *
     * @param request HTTP请求对象
     * @return 浏览器名称，如"Chrome"、"Firefox"、"Safari"等
     */
    public static String getBrowser(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if ("unknown".equals(userAgent)) {
            return "unknown";
        }

        // 简单的浏览器识别逻辑
        if (userAgent.contains("Edg/")) {
            return "Edge";
        } else if (userAgent.contains("Chrome/") && !userAgent.contains("Edg/")) {
            return "Chrome";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            return "Safari";
        } else if (userAgent.contains("Firefox/")) {
            return "Firefox";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "Internet Explorer";
        } else if (userAgent.contains("MicroMessenger")) {
            return "微信浏览器";
        } else if (userAgent.contains("DingTalk")) {
            return "钉钉";
        } else {
            return "其他浏览器";
        }
    }

    /**
     * 获取当前请求的浏览器信息
     *
     * @return 浏览器名称
     */
    public static String getBrowser() {
        HttpServletRequest request = getRequest();
        return getBrowser(request);
    }

    /**
     * 从User-Agent中提取操作系统信息
     *
     * @param request HTTP请求对象
     * @return 操作系统名称，如"Windows"、"macOS"、"Linux"、"Android"、"iOS"等
     */
    public static String getOs(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if ("unknown".equals(userAgent)) {
            return "unknown";
        }

        // 简单的操作系统识别逻辑
        if (userAgent.contains("Windows NT")) {
            return "Windows";
        } else if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS X")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else {
            return "其他操作系统";
        }
    }

    /**
     * 获取当前请求的操作系统信息
     *
     * @return 操作系统名称
     */
    public static String getOs() {
        HttpServletRequest request = getRequest();
        return getOs(request);
    }

    /**
     * 获取请求的完整URL
     *
     * @return 完整URL
     */
    public static String getRequestURL() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }

        StringBuffer url = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    /**
     * 获取请求的URI
     *
     * @return 请求URI
     */
    public static String getRequestURI() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        return request.getRequestURI();
    }

    /**
     * 获取请求的方法（GET/POST/PUT/DELETE等）
     *
     * @return 请求方法
     */
    public static String getRequestMethod() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        return request.getMethod();
    }

    /**
     * 禁用浏览器缓存
     * 设置响应头以防止浏览器缓存响应内容
     *
     * @param response HTTP响应对象
     */
    public static void disableCache(HttpServletResponse response) {
        if (response == null) {
            return;
        }

        // HTTP 1.1 缓存控制
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        // HTTP 1.0 缓存控制
        response.setHeader("Pragma", "no-cache");

        // 设置过期时间为立即过期
        response.setHeader("Expires", "0");
    }

    /**
     * 启用浏览器缓存
     *
     * @param response HTTP响应对象
     * @param maxAge 缓存时间（秒）
     */
    public static void enableCache(HttpServletResponse response, int maxAge) {
        if (response == null) {
            return;
        }

        response.setHeader("Cache-Control", "max-age=" + maxAge);
    }

    /**
     * 设置公共缓存（可以被任何缓存器缓存）
     *
     * @param response HTTP响应对象
     * @param maxAge 缓存时间（秒）
     */
    public static void setPublicCache(HttpServletResponse response, int maxAge) {
        if (response == null) {
            return;
        }

        response.setHeader("Cache-Control", "public, max-age=" + maxAge);
    }

    /**
     * 设置私有缓存（只能被浏览器缓存，不能被共享缓存器缓存）
     *
     * @param response HTTP响应对象
     * @param maxAge 缓存时间（秒）
     */
    public static void setPrivateCache(HttpServletResponse response, int maxAge) {
        if (response == null) {
            return;
        }

        response.setHeader("Cache-Control", "private, max-age=" + maxAge);
    }
}