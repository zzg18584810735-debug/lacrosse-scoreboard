package com.edus.controller;

import com.edus.common.utils.UserContext;
import com.edus.common.utils.UserContext.UserDto;
import com.edus.service.TokenService;
import com.edus.util.TokenUtil;
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

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        // TODO: 这里应该查询数据库验证用户
        // 现在只是演示，简单验证
        if ("admin".equals(username) && "123456".equals(password)) {
            // 创建UserDto
            String userId = "1"; // 模拟用户ID
            UserDto userDto = new UserDto(userId, username);

            // 生成token（只生成JWT）
            String token = TokenUtil.generateToken(userDto);

            // 存储token到Redis
            tokenService.saveToken(userId, token);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("token", token);
            result.put("userId", userId);
            result.put("username", username);

            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", "用户名或密码错误");
        return result;
    }

    /**
     * 获取当前用户信息
     * 需要token认证
     */
    @GetMapping("/userInfo")
    public Map<String, Object> getUserInfo() {
        UserDto userDto = UserContext.get();

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取用户信息成功");

        if (userDto != null) {
            result.put("userId", userDto.getId());
            result.put("username", userDto.getName());
        }

        return result;
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Map<String, Object> logout() {
        UserDto userDto = UserContext.get();
        if (userDto != null) {
            tokenService.removeToken(userDto.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退出登录成功");

        return result;
    }
}