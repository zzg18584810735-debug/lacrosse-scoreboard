package com.edus.security.monitor;

import com.edus.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * 监控接口访问保护。
 */
@Component
public class MonitorAccessGuard {

    private static final String MONITOR_TOKEN_HEADER = "X-Monitor-Token";

    private final Environment environment;
    private final String accessToken;
    private final boolean allowDevProfile;
    private final boolean allowInternalNetwork;

    public MonitorAccessGuard(
            Environment environment,
            @Value("${edus.monitor.access-token:}") String accessToken,
            @Value("${edus.monitor.allow-dev-profile:false}") boolean allowDevProfile,
            @Value("${edus.monitor.allow-internal-network:true}") boolean allowInternalNetwork) {
        this.environment = environment;
        this.accessToken = accessToken;
        this.allowDevProfile = allowDevProfile;
        this.allowInternalNetwork = allowInternalNetwork;
    }

    public void requireReadAccess(HttpServletRequest request) {
        requireAccess(request);
    }

    public void requireWriteAccess(HttpServletRequest request) {
        requireAccess(request);
    }

    private void requireAccess(HttpServletRequest request) {
        if (isDevOrTestProfileAllowed()) {
            return;
        }

        if (hasValidMonitorToken(request)) {
            return;
        }

        if (allowInternalNetwork && isInternalRequest(request)) {
            return;
        }

        throw new BusinessException(403, "无权访问监控接口");
    }

    private boolean isDevOrTestProfileAllowed() {
        if (!allowDevProfile) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("dev") || profile.equals("test"));
    }

    private boolean hasValidMonitorToken(HttpServletRequest request) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        String token = request.getHeader(MONITOR_TOKEN_HEADER);
        return accessToken.equals(token);
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return isInternalIp(clientIp);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean isInternalIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        return ip.equals("127.0.0.1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || isPrivate172Ip(ip);
    }

    private boolean isPrivate172Ip(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        try {
            int secondSegment = Integer.parseInt(parts[1]);
            return secondSegment >= 16 && secondSegment <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
