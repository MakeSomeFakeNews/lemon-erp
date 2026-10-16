package com.lemon.erp.framework.config;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/config", "/auth/captcha", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, WebMvcConfig.FILE_URL_PREFIX + "**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, resp, ex) ->
                                writeJson(resp, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        R.fail(R.UNAUTHORIZED, "未登录或登录已过期")))
                        .accessDeniedHandler((req, resp, ex) ->
                                writeJson(resp, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                        R.fail(R.FORBIDDEN, "没有权限，请联系管理员"))))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeJson(HttpServletResponse resp, ObjectMapper mapper, int status, R<?> body) throws IOException {
        resp.setStatus(status);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(mapper.writeValueAsString(body));
    }
}
