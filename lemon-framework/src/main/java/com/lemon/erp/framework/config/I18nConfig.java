package com.lemon.erp.framework.config;

import com.lemon.erp.common.i18n.Messages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.IOException;

/**
 * 国际化：消息资源包（{@link Messages}，各模块的 i18n/*.properties）、按请求头 Accept-Language 确定语言（默认简体中文）。
 * Spring MVC 处理请求时由 LocaleResolver 设置语言；安全过滤器等在 MVC 之前输出的提示，由这里的过滤器提前按同一规则设置
 */
@Configuration
public class I18nConfig {

    /** RequestContextFilter（-105，按 request.getLocale() 设置，没有请求头时为服务器的系统语言）之后、安全过滤器（-100）之前 */
    private static final int LOCALE_FILTER_ORDER = -104;

    @Bean
    public MessageSource messageSource() {
        return Messages.messageSource();
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(Messages.SUPPORTED_LOCALES);
        resolver.setDefaultLocale(Messages.DEFAULT_LOCALE);
        return resolver;
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> localeContextFilter(LocaleResolver localeResolver) {
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
                try {
                    chain.doFilter(request, response);
                } finally {
                    LocaleContextHolder.resetLocaleContext();
                }
            }
        });
        bean.setOrder(LOCALE_FILTER_ORDER);
        return bean;
    }
}
