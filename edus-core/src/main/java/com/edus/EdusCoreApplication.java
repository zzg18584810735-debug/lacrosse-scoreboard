package com.edus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.edus.mapper")
public class EdusCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdusCoreApplication.class, args);
        System.out.println("========================================");
        System.out.println("edus-core 应用启动成功！");
        System.out.println("分页和SQL拦截功能已启用");
        System.out.println("========================================");
    }
}
