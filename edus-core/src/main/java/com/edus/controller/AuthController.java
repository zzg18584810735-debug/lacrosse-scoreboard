package com.edus.controller;

import com.edus.common.utils.UserContext;
import com.edus.common.utils.UserContext.UserDto;
import com.edus.common.utils.Result;
import com.edus.service.TokenService;
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

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
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

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", userId);
            data.put("username", username);

            return Result.ok("登录成功", data);
        }

        return Result.error("用户名或密码错误");
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