package com.edus.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.edus.common.enums.StatusCode;
import com.edus.common.utils.Result;
import com.edus.common.utils.TokenUtil;
import com.edus.common.utils.UserContext;
import com.edus.common.utils.UserContext.UserDto;
import com.edus.auth.service.SignatureService;
import com.edus.auth.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final SignatureService signatureService;
    private final boolean scoreboardPermitAll;

    public LoginInterceptor(TokenService tokenService, SignatureService signatureService,
                            @Value("${edus.auth.scoreboard-permit-all:false}") boolean scoreboardPermitAll) {
        this.tokenService = tokenService;
        this.signatureService = signatureService;
        this.scoreboardPermitAll = scoreboardPermitAll;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String servletPath = request.getServletPath();
        if (scoreboardPermitAll && ("/scoreboard".equals(servletPath)
                || StrUtil.startWith(servletPath, "/scoreboard/"))) {
            UserDto anonymous = new UserDto("0", "scoreboard-dev");
            setUserContext(request, anonymous);
            request.setAttribute("authType", "scoreboard-permit-all");
            return true;
        }

        String token = request.getHeader(TokenUtil.getTokenHeader());
        if (StrUtil.isBlank(token)) {
            return validateSignatureAccess(request, response);
        }

        UserDto userDto = tokenService.validateAndGetUser(token);
        if (userDto == null) {
            return handleUnauthorized(response, StatusCode.TOKEN_INVALID);
        }

        setUserContext(request, userDto);
        return true;
    }

    private boolean validateSignatureAccess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (signatureService.validate(request)) {
            UserDto signUser = new UserDto("0", request.getHeader(SignatureService.HEADER_APP_ID));
            setUserContext(request, signUser);
            request.setAttribute("authType", "signature");
            return true;
        }

        StatusCode statusCode = signatureService.hasSignatureHeaders(request)
                ? StatusCode.SIGNATURE_INVALID
                : StatusCode.LOGIN_EXPIRED;
        return handleUnauthorized(response, statusCode);
    }

    private void setUserContext(HttpServletRequest request, UserDto userDto) {
        UserContext.set(userDto);
        request.setAttribute("userId", userDto.getId());
        request.setAttribute("username", userDto.getName());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.remove();
    }

    private boolean handleUnauthorized(HttpServletResponse response, StatusCode statusCode) throws Exception {
        response.setStatus(statusCode.getCode());
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.error(statusCode);
        response.getWriter().write(JSONUtil.toJsonStr(result));
        return false;
    }
}
