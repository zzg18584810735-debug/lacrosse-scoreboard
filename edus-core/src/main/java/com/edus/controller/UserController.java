package com.edus.controller;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.edus.common.enums.StatusCode;
import com.edus.common.page.PageUtils;
import com.edus.common.utils.Result;
import com.edus.entity.User;
import com.edus.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return Result.ok(createdUser, "用户创建成功");
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.ok(user);
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public Result<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return Result.ok(user);
    }

    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public Result<PageUtils> getPage(@RequestParam Map<String, Object> params) {
        PageUtils pageUtils = userService.queryPage(params);
        return Result.ok(pageUtils);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        return Result.ok(updatedUser, "用户更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok("用户删除成功");
    }

    /**
     * Hutool 工具演示
     */
    @GetMapping("/demo/hutool")
    public Result<Map<String, Object>> hutoolDemo() {
        Map<String, Object> result = new HashMap<>();

        String phone = "13800138000";
        String email = "user@example.com";

        result.put("original_phone", phone);
        result.put("masked_phone", DesensitizedUtil.mobilePhone(phone));
        result.put("original_email", email);
        result.put("masked_email", DesensitizedUtil.email(email));
        result.put("md5_demo", DigestUtil.md5Hex("hello"));
        result.put("sha256_demo", DigestUtil.sha256Hex("hello"));
        result.put("is_numeric_demo", StrUtil.isNumeric("12345"));
        result.put("is_empty_demo", StrUtil.isEmpty(""));

        return Result.ok(result);
    }
}