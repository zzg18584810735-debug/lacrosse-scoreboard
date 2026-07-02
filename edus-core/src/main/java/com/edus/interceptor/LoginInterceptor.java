package com.edus.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.edus.common.utils.Result;
import com.edus.common.utils.UserContext;
import com.edus.common.utils.UserContext.UserDto;
import com.edus.service.TokenService;
import com.edus.common.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 验证用户token是否有效
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 处理跨域预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取token
        String token = request.getHeader(TokenUtil.getTokenHeader());

        if (StrUtil.isBlank(token)) {
            return handleUnauthorized(response, "未登录或token已过期");
        }

        // 验证token并获取用户信息
        UserDto userDto = tokenService.validateAndGetUser(token);

        if (userDto == null) {
            return handleUnauthorized(response, "token验证失败");
        }

        // 将用户信息存储到ThreadLocal中，方便后续使用
        UserContext.set(userDto);
        request.setAttribute("userId", userDto.getId());
        request.setAttribute("username", userDto.getName());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清理ThreadLocal，防止内存泄漏
        UserContext.remove();
    }

    /**
     * 处理未授权请求
     */
    private boolean handleUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.error(401, message);
        response.getWriter().write(JSONUtil.toJsonStr(result));
        return false;
    }
}