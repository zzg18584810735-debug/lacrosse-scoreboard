package com.edus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan({"com.edus.auth.mapper", "com.edus.example.mapper", "com.edus.score.mapper"})
public class EdusCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdusCoreApplication.class, args);
        System.out.println("========================================");
        System.out.println("edus-core 应用启动成功！");
        System.out.println("========================================");
    }
}
