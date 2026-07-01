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
        return Result.ok("用户创建成功", createdUser);
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
        return Result.ok( "用户更新成功", updatedUser);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.ok("用户删除成功");
    }
}