package com.lemon.erp.framework.tenant;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfig {

    @Bean
    public TenantTaskDecorator tenantTaskDecorator() {
        return new TenantTaskDecorator();
    }
}
