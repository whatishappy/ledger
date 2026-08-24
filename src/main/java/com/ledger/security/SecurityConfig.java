package com.ledger.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.result.Result;
import com.ledger.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;

import java.util.List;

/**
 * Spring Security 6.x 配置
 * - 禁用 CSRF（无状态API）
 * - 放行公开路径
 * - 注册 JWT 认证过滤器
 * - 禁用默认登录页
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    // BCrypt 强度系数10
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF
                .csrf(csrf -> csrf.disable())
                // 启用 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态会话
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行所有内部转发/异步/错误 dispatch：SSE 流式响应提交后若触发 error dispatch，
                        // 此时 response 已提交，Security 再拒绝会报 "response already committed"。
                        // 实际观察 error 可经 ASYNC/INCLUDE/ERROR/FORWARD 任意路径进入，故全部放行；
                        // 只有真正的 REQUEST（客户端请求）才执行认证
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD,
                                DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()
                        // 公开路径
                        .requestMatchers(
                                "/api/user/register",
                                "/api/user/login",
                                "/api/auth/refresh"
                        ).permitAll()
                        // 接口文档
                        .requestMatchers(
                                "/doc.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()
                        // 监控端点（仅内部访问）
                        .requestMatchers("/actuator/**").permitAll()
                        // 其余需要认证
                        .anyRequest().authenticated()
                )
                // 异常处理
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);
                            response.getWriter().write(objectMapper.writeValueAsString(result));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            Result<Void> result = Result.fail(ResultCode.FORBIDDEN);
                            response.getWriter().write(objectMapper.writeValueAsString(result));
                        })
                )
                // 注册 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
