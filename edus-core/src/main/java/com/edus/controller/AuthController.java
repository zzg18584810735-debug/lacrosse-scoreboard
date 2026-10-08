package com.edus.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.edus.aop.annotation.LogOperation;
import com.edus.aop.annotation.RateLimiter;
import com.edus.common.enums.StatusCode;
import com.edus.common.exception.BusinessException;
import com.edus.common.utils.UserContext;
import com.edus.common.utils.UserContext.UserDto;
import com.edus.common.utils.Result;
import com.edus.entity.User;
import com.edus.service.UserService;
import com.edus.service.auth.TokenService;
import com.edus.common.utils.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、退出等认证相关功能
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;
    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @RateLimiter(key = "auth_login", timeout = 60, limit = 10, limitType = RateLimiter.RateLimiterType.IP)
    @LogOperation(
            operationType = "LOGIN",
            module = "登录",
            description = "登录",
            logParams = false,
            logResponse = true
    )
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        if (StrUtil.hasBlank(username, password)) {
            throw BusinessException.validationError("用户名和密码不能为空");
        }

        User user = userService.getUserByUsernameForAuth(username);
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(StatusCode.FORBIDDEN, "用户已被禁用");
        }

        if (!matchesPassword(password, user.getPassword())) {
            throw new BusinessException(StatusCode.INVALID_CREDENTIALS);
        }

        UserDto userDto = new UserDto(String.valueOf(user.getId()), user.getUsername());
        String token = TokenUtil.generateToken(userDto);
        tokenService.saveToken(String.valueOf(user.getId()), token);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("userId", user.getId());
        data.put("username", user.getUsername());

        return Result.ok("登录成功", data);
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        return BCrypt.checkpw(rawPassword, storedPassword);
    }

    /**
     * 获取当前用户信息
     * 需要token认证
     */
    @GetMapping("/userInfo")
    public Result<Map<String, Object>> getUserInfo() {
        UserDto userDto = UserContext.get();

        if (userDto != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userDto.getId());
            data.put("username", userDto.getName());
            return Result.ok("获取用户信息成功", data);
        }

        return Result.error("获取用户信息失败");
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        UserDto userDto = UserContext.get();
        if (userDto != null) {
            tokenService.removeToken(userDto.getId());
        }

        return Result.ok("退出登录成功");
    }
}
