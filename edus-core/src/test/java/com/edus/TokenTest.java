package com.edus;

import cn.hutool.core.util.StrUtil;
import com.edus.common.utils.UserContext.UserDto;
import com.edus.util.TokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Token工具类测试
 */
@SpringBootTest
public class TokenTest {

    @Test
    public void testGenerateAndValidateToken() {
        // 测试生成token
        String userId = "1";
        String username = "admin";
        UserDto userDto = new UserDto(userId, username);

        String token = TokenUtil.generateToken(userDto);
        System.out.println("生成的Token: " + token);

        // 测试验证token
        boolean isValid = TokenUtil.validateToken(token);
        System.out.println("Token验证结果: " + isValid);

        // 测试获取UserDto
        UserDto extractedUserDto = TokenUtil.getUserDtoFromToken(token);
        System.out.println("提取的UserDto: " + extractedUserDto);

        // 测试获取用户ID
        String extractedUserId = TokenUtil.getUserIdFromToken(token);
        System.out.println("提取的用户ID: " + extractedUserId);

        // 测试获取用户名
        String extractedUsername = TokenUtil.getUsernameFromToken(token);
        System.out.println("提取的用户名: " + extractedUsername);

        // 断言
        assert isValid : "Token验证应该成功";
        assert userId.equals(extractedUserId) : "用户ID应该匹配";
        assert username.equals(extractedUsername) : "用户名应该匹配";
        assert extractedUserDto != null : "UserDto不应该为空";
    }

    @Test
    public void testInvalidToken() {
        // 测试无效token
        String invalidToken = "invalid.token.string";

        boolean isValid = TokenUtil.validateToken(invalidToken);
        System.out.println("无效Token验证结果: " + isValid);

        assert !isValid : "无效Token验证应该失败";
    }

    @Test
    public void testTokenWithBearer() {
        // 测试带Bearer前缀的token
        String userId = "2";
        String username = "user";
        UserDto userDto = new UserDto(userId, username);

        String token = TokenUtil.generateToken(userDto);
        String bearerToken = TokenUtil.getTokenPrefix() + token;

        System.out.println("带前缀的Token: " + bearerToken);

        // 测试验证带前缀的token
        boolean isValid = TokenUtil.validateToken(bearerToken);
        System.out.println("带前缀Token验证结果: " + isValid);

        assert isValid : "带前缀Token验证应该成功";

        // 测试从带前缀token中获取用户信息
        UserDto extractedUserDto = TokenUtil.getUserDtoFromToken(bearerToken);
        String extractedUserId = TokenUtil.getUserIdFromToken(bearerToken);
        String extractedUsername = TokenUtil.getUsernameFromToken(bearerToken);

        assert extractedUserDto != null : "UserDto不应该为空";
        assert userId.equals(extractedUserId) : "用户ID应该匹配";
        assert username.equals(extractedUsername) : "用户名应该匹配";
    }

    @Test
    public void testRefreshToken() {
        // 测试刷新token
        String userId = "3";
        String username = "testuser";
        UserDto userDto = new UserDto(userId, username);

        String oldToken = TokenUtil.generateToken(userDto);
        System.out.println("原始Token: " + oldToken);

        String newToken = TokenUtil.refreshToken(oldToken);
        System.out.println("刷新后的Token: " + newToken);

        assert !StrUtil.isBlank(newToken) : "刷新Token应该成功";
        assert !oldToken.equals(newToken) : "新Token应该与旧Token不同";

        // 验证新token有效
        boolean isValid = TokenUtil.validateToken(newToken);
        assert isValid : "新Token验证应该成功";
    }

    @Test
    public void testRemoveToken() {
        // 测试删除token
        String userId = "4";
        String username = "removetest";
        UserDto userDto = new UserDto(userId, username);

        String token = TokenUtil.generateToken(userDto);
        System.out.println("生成的Token: " + token);

        // 验证token有效
        boolean isValidBefore = TokenUtil.validateToken(token);
        System.out.println("删除前Token验证结果: " + isValidBefore);

        // 删除token
        TokenUtil.removeToken(userId);

        // 验证token无效
        boolean isValidAfter = TokenUtil.validateToken(token);
        System.out.println("删除后Token验证结果: " + isValidAfter);

        assert isValidBefore : "删除前Token验证应该成功";
        assert !isValidAfter : "删除后Token验证应该失败";
    }
}