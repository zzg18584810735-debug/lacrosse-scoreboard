package com.edus;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.Test;

public class PasswordTest {

    @Test
    public void testPasswordEncryption() {
        // 测试原始密码
        String password = "123456";
        System.out.println("原始密码: " + password);

        // MD5 加密
        String md5Password = SecureUtil.md5(password);
        System.out.println("MD5加密: " + md5Password);

        // SHA-256 加密
        String sha256Password = SecureUtil.sha256(password);
        System.out.println("SHA-256加密: " + sha256Password);

        // BCrypt 加密（推荐用于密码存储）
        String bcrytPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String bcrytPassword1 = BCrypt.hashpw("123456", BCrypt.gensalt());
        System.out.println(bcrytPassword1+ "BCrypt加密: " + bcrytPassword);

        // 验证 BCrypt 密码
        boolean isMatch = BCrypt.checkpw(password, bcrytPassword);
        System.out.println("BCrypt密码验证: " + isMatch);

        // 验证错误密码
        boolean isMatch2 = BCrypt.checkpw("wrongpassword", bcrytPassword);
        System.out.println("错误密码验证: " + isMatch2);
    }
}
