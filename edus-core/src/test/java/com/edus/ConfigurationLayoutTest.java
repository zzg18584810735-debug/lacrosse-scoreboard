package com.edus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 环境配置分层测试。
 */
class ConfigurationLayoutTest {

    private static final List<String> CONNECTION_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.data.redis.host",
            "spring.data.redis.port",
            "spring.data.redis.password",
            "spring.data.redis.database"
    );

    @Test
    void 公共配置统一读取数据库和Redis环境变量() throws IOException {
        List<PropertySource<?>> sources = load("application.yml");

        assertEquals(
                "jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&useSSL=${DB_USE_SSL:false}&serverTimezone=${DB_TIMEZONE:UTC}",
                property(sources, "spring.datasource.url")
        );
        for (String propertyName : CONNECTION_PROPERTIES) {
            assertNotNull(property(sources, propertyName), propertyName + " 应在 application.yml 中定义");
        }
    }

    @Test
    void 环境配置只保留连接池差异() throws IOException {
        for (String profile : List.of("dev", "test", "prod")) {
            List<PropertySource<?>> sources = load("application-" + profile + ".yml");
            for (String propertyName : CONNECTION_PROPERTIES) {
                assertNull(property(sources, propertyName), propertyName + " 不应在 " + profile + " 环境重复定义");
            }
            assertNotNull(
                    property(sources, "spring.datasource.hikari.maximum-pool-size"),
                    profile + " 环境应定义数据库连接池容量"
            );
            assertNotNull(
                    property(sources, "spring.data.redis.lettuce.pool.max-active"),
                    profile + " 环境应定义 Redis 连接池容量"
            );
        }
    }

    private List<PropertySource<?>> load(String resourceName) throws IOException {
        return new YamlPropertySourceLoader().load(resourceName, new ClassPathResource(resourceName));
    }

    private Object property(List<PropertySource<?>> sources, String propertyName) {
        for (PropertySource<?> source : sources) {
            Object value = source.getProperty(propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
