package com.ledger.common.config;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 从 .env.development 文件加载环境变量到 Spring Environment
 * 启动优先级高于 @Value，不依赖外部库（零新增依赖）
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       org.springframework.boot.SpringApplication application) {
        Path envFile = Path.of(".env.development");
        if (!Files.exists(envFile)) {
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        try (Stream<String> lines = Files.lines(envFile)) {
            lines.filter(line -> !line.isBlank() && !line.trim().startsWith("#"))
                    .filter(line -> line.contains("="))
                    .forEach(line -> {
                        int idx = line.indexOf('=');
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        // 去掉首尾引号
                        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        properties.put(key, value);
                    });
        } catch (IOException e) {
            System.err.println("[WARN] Failed to load .env.development: " + e.getMessage());
            return;
        }
        if (properties.isEmpty()) {
            return;
        }
        MapPropertySource propertySource = new MapPropertySource("dotenv", properties);
        environment.getPropertySources().addFirst(propertySource);
    }
}
