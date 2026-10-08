package com.edus.controller.example;

import com.edus.aop.annotation.RateLimiter;
import com.edus.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 限流功能示例控制器
 * 演示如何使用@RateLimiter注解实现接口限流
 *
 * @author edus
 */
@Slf4j
@RestController
@RequestMapping("/api/rate-limiter")
public class RateLimiterExampleController {

    /**
     * 示例1：全局限流
     * 所有用户总共在60秒内只能访问100次
     */
    @RateLimiter(timeout = 60, limit = 100, limitType = RateLimiter.RateLimiterType.DEFAULT)
    @GetMapping("/global")
    public Result<String> globalLimiter() {
        log.info("全局限流示例被访问");
        return Result.ok("全局限流示例：所有用户在60秒内总共只能访问100次");
    }

    /**
     * 示例2：基于IP的限流
     * 同一个IP在10秒内只能访问5次
     */
    @RateLimiter(timeout = 10, limit = 5, limitType = RateLimiter.RateLimiterType.IP)
    @GetMapping("/by-ip")
    public Result<String> ipBasedLimiter() {
        log.info("基于IP的限流示例被访问");
        return Result.ok("基于IP的限流：同一IP在10秒内只能访问5次");
    }

    /**
     * 示例3：基于用户的限流
     * 同一个用户在30秒内只能访问10次（需要登录）
     */
    @RateLimiter(timeout = 30, limit = 10, limitType = RateLimiter.RateLimiterType.USER)
    @GetMapping("/by-user")
    public Result<String> userBasedLimiter() {
        log.info("基于用户的限流示例被访问");
        return Result.ok("基于用户的限流：同一用户在30秒内只能访问10次");
    }

    /**
     * 示例4：自定义key的限流
     * 使用自定义的限流key前缀
     */
    @RateLimiter(key = "custom_api", timeout = 60, limit = 20)
    @GetMapping("/custom-key")
    public Result<String> customKeyLimiter() {
        log.info("自定义key的限流示例被访问");
        return Result.ok("自定义key的限流：在60秒内只能访问20次");
    }

    /**
     * 示例5：严格限流
     * 用于登录接口的严格限流，防止暴力破解
     */
    @RateLimiter(key = "login_attempt", timeout = 300, limit = 3, limitType = RateLimiter.RateLimiterType.IP)
    @PostMapping("/login-example")
    public Result<String> loginLimiterExample(@RequestParam String username) {
        log.info("登录接口限流示例被访问，用户：{}", username);
        return Result.ok("登录限流示例：同一IP在5分钟内只能尝试3次登录");
    }

    /**
     * 示例6：高频接口限流
     * 用于高频率查询接口的限流
     */
    @RateLimiter(timeout = 1, limit = 10, limitType = RateLimiter.RateLimiterType.USER)
    @GetMapping("/high-frequency")
    public Result<String> highFrequencyLimiter() {
        log.info("高频接口限流示例被访问");
        return Result.ok("高频接口限流：同一用户在1秒内只能访问10次");
    }
}