package com.edus.controller.example;

import com.edus.aop.annotation.Idempotent;
import com.edus.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 接口幂等性示例控制器
 * 演示@Idempotent注解的使用方法，防止重复提交
 *
 * @author edus
 */
@Slf4j
@RestController
@RequestMapping("/api/idempotent")
public class IdempotentExampleController {

    // ==================== 基础幂等性示例 ====================

    /**
     * 示例1：基础用法 - 基于方法路径自动幂等
     * 同一个请求在5秒内只能执行一次
     */
    @Idempotent(timeout = 5, message = "订单创建处理中，请勿重复提交")
    @PostMapping("/order/create")
    public Result<String> createOrder(@RequestBody String orderData) {
        log.info("创建订单：{}", orderData);
        // 模拟订单创建
        return Result.ok("订单创建成功：" + orderData);
    }

    /**
     * 示例2：基于业务ID的幂等
     * 同一个订单ID在10秒内只能更新一次
     */
    @Idempotent(key = "#orderId", timeout = 10, message = "订单更新处理中，请勿重复提交")
    @PutMapping("/order/{orderId}")
    public Result<String> updateOrder(@PathVariable String orderId, @RequestBody String orderData) {
        log.info("更新订单：orderId={}, data={}", orderId, orderData);
        // 模拟订单更新
        return Result.ok("订单更新成功：" + orderId);
    }

    /**
     * 示例3：基于用户ID的幂等
     * 3秒内同一用户只能发起一次支付请求
     */
    @Idempotent(keyType = Idempotent.IdempotentKeyType.USER, timeout = 3, message = "支付处理中，请勿重复提交")
    @PostMapping("/payment/pay")
    public Result<String> pay(@RequestBody String paymentData) {
        log.info("发起支付：{}", paymentData);
        // 模拟支付处理
        return Result.ok("支付发起成功");
    }

    /**
     * 示例4：严格防重模式
     * 删除用户等敏感操作，60秒内防重
     */
    @Idempotent(timeout = 60, message = "敏感操作处理中，请勿重复提交")
    @DeleteMapping("/user/delete")
    public Result<String> deleteUser(@RequestBody Long userId) {
        log.info("删除用户：{}", userId);
        // 模拟用户删除
        return Result.ok("用户删除成功：" + userId);
    }

    /**
     * 示例5：高频接口短时间防重
     * 同一用户1秒内只能添加一条评论
     */
    @Idempotent(timeout = 1, keyType = Idempotent.IdempotentKeyType.USER, message = "评论提交中，请勿重复提交")
    @PostMapping("/comment/add")
    public Result<String> addComment(@RequestBody String commentData) {
        log.info("添加评论：{}", commentData);
        // 模拟评论添加
        return Result.ok("评论添加成功");
    }

    /**
     * 示例6：基于IP的幂等
     * 同一IP在30秒内只能提交一次反馈
     */
    @Idempotent(keyType = Idempotent.IdempotentKeyType.IP, timeout = 30, message = "反馈提交中，请勿重复提交")
    @PostMapping("/feedback/submit")
    public Result<String> submitFeedback(@RequestBody String feedbackData) {
        log.info("提交反馈：{}", feedbackData);
        // 模拟反馈提交
        return Result.ok("反馈提交成功");
    }

    // ==================== 实际业务场景示例 ====================

    /**
     * 示例7：密码修改接口
     * 密码修改是敏感操作，需要严格防重
     */
    @Idempotent(keyType = Idempotent.IdempotentKeyType.USER, timeout = 10,
               message = "密码修改中，请勿重复提交")
    @PutMapping("/user/password")
    public Result<String> updatePassword(@RequestBody String passwordData) {
        log.info("修改密码");
        // 模拟密码修改
        return Result.ok("密码修改成功");
    }

    /**
     * 示例8：邮件发送接口
     * 防止用户频繁点击发送邮件按钮
     */
    @Idempotent(keyType = Idempotent.IdempotentKeyType.USER, timeout = 60,
               message = "邮件发送中，请勿重复提交")
    @PostMapping("/email/send")
    public Result<String> sendEmail(@RequestBody String emailData) {
        log.info("发送邮件：{}", emailData);
        // 模拟邮件发送
        return Result.ok("邮件发送成功");
    }

    /**
     * 示例9：文件上传接口
     * 防止用户重复上传大文件
     */
    @Idempotent(keyType = Idempotent.IdempotentKeyType.USER, timeout = 30,
               message = "文件上传中，请勿重复提交")
    @PostMapping("/file/upload")
    public Result<String> uploadFile(@RequestBody String fileData) {
        log.info("上传文件：{}", fileData);
        // 模拟文件上传
        return Result.ok("文件上传成功");
    }

    /**
     * 示例10：优惠券领取接口
     * 防止用户重复领取同一优惠券
     */
    @Idempotent(key = "#couponId", keyType = Idempotent.IdempotentKeyType.USER, timeout = 3,
               message = "优惠券领取中，请勿重复点击")
    @PostMapping("/coupon/{couponId}/claim")
    public Result<String> claimCoupon(@PathVariable String couponId) {
        log.info("领取优惠券：{}", couponId);
        // 模拟优惠券领取
        return Result.ok("优惠券领取成功：" + couponId);
    }

    /**
     * 示例11：表单提交接口
     * 通用表单提交，防止用户重复点击
     */
    @Idempotent(timeout = 5, message = "表单提交中，请勿重复提交")
    @PostMapping("/form/submit")
    public Result<String> submitForm(@RequestBody String formData) {
        log.info("提交表单：{}", formData);
        // 模拟表单处理
        return Result.ok("表单提交成功");
    }

    /**
     * 示例12：投票接口
     * 防止刷票行为
     */
    @Idempotent(key = "#voteId", keyType = Idempotent.IdempotentKeyType.USER, timeout = 10,
               message = "投票处理中，请勿重复投票")
    @PostMapping("/vote/{voteId}")
    public Result<String> vote(@PathVariable String voteId) {
        log.info("投票：{}", voteId);
        // 模拟投票处理
        return Result.ok("投票成功：" + voteId);
    }

    // ==================== 错误处理示例 ====================

    /**
     * 示例13：业务失败自动删除幂等性Key
     * 演示业务执行失败时，允许用户重新尝试
     */
    @Idempotent(timeout = 5, message = "操作处理中，请勿重复提交")
    @PostMapping("/operation/failable")
    public Result<String> failableOperation(@RequestBody boolean shouldFail) {
        log.info("执行可能失败的操作：shouldFail={}", shouldFail);

        if (shouldFail) {
            // 业务执行失败，幂等性Key会被自动删除，允许重试
            throw new RuntimeException("业务执行失败");
        }

        return Result.ok("操作执行成功");
    }

    // ==================== 幂等性测试接口 ====================

    /**
     * 幂等性测试接口
     * 用于测试幂等性功能是否正常工作
     */
    @GetMapping("/test")
    public Result<String> idempotentTest() {
        return Result.ok("幂等性功能已集成，请使用POST接口测试防重功能");
    }

    /**
     * 幂等性测试说明
     */
    @GetMapping("/test/info")
    public Result<IdempotentInfo> getIdempotentInfo() {
        IdempotentInfo info = new IdempotentInfo();
        info.setDescription("接口幂等性功能演示");
        info.setUsage("使用@Idempotent注解标记需要防重的接口");
        info.setTestSteps("1. 快速多次点击同一接口  2. 观察防重效果");
        info.setRedisKeyPrefix("idempotent:");
        return Result.ok(info);
    }

    /**
     * 幂等性信息数据类
     */
    public static class IdempotentInfo {
        private String description;
        private String usage;
        private String testSteps;
        private String redisKeyPrefix;

        // getter/setter省略...
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUsage() { return usage; }
        public void setUsage(String usage) { this.usage = usage; }
        public String getTestSteps() { return testSteps; }
        public void setTestSteps(String testSteps) { this.testSteps = testSteps; }
        public String getRedisKeyPrefix() { return redisKeyPrefix; }
        public void setRedisKeyPrefix(String redisKeyPrefix) { this.redisKeyPrefix = redisKeyPrefix; }
    }
}