package com.ledger.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI + Knife4j 分组配置
 * - 声明全局 Info 信息与 Bearer 全局鉴权
 * - 按业务模块拆分 6 个分组：用户/认证/账目/预算/统计/导出
 *   → Knife4j 顶部下拉可切换，左侧菜单显示对应模块下的接口 + Swagger Models
 */
@Configuration
public class OpenApiConfig {

    private static final String AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人云端记账本 API")
                        .version("1.0.0")
                        .description("基于 Spring Boot 3.5 + MyBatis-Plus + Redisson + Knife4j 的个人云端记账本接口文档")
                        .contact(new Contact().name("ledger").email("support@ledger.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(AUTH_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("登录后获取 accessToken，填入格式：Bearer {token}")))
                // 所有接口默认需要 Bearer 鉴权（登录/注册/刷新等公开接口 Controller 上可单独标注 no auth）
                .addSecurityItem(new SecurityRequirement().addList(AUTH_SCHEME));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户模块")
                .packagesToScan("com.ledger.modules.user.controller")
                .pathsToMatch("/api/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证模块")
                .packagesToScan("com.ledger.modules.user.controller")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi accountApi() {
        return GroupedOpenApi.builder()
                .group("账目模块")
                .packagesToScan("com.ledger.modules.account.controller")
                .pathsToMatch("/api/account/**")
                .build();
    }

    @Bean
    public GroupedOpenApi budgetApi() {
        return GroupedOpenApi.builder()
                .group("预算模块")
                .packagesToScan("com.ledger.modules.budget.controller")
                .pathsToMatch("/api/budget/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statisticsApi() {
        return GroupedOpenApi.builder()
                .group("统计模块")
                .packagesToScan("com.ledger.modules.statistics.controller")
                .pathsToMatch("/api/statistics/**")
                .build();
    }

    @Bean
    public GroupedOpenApi exportApi() {
        return GroupedOpenApi.builder()
                .group("导出模块")
                .packagesToScan("com.ledger.modules.export.controller")
                .pathsToMatch("/api/export/**")
                .build();
    }

    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/api/**")
                .build();
    }
}
