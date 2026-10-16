package com.lemon.erp.framework.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    public static final String FILE_URL_PREFIX = "/files/";

    private final UploadProperties uploadProperties;

    /** 上传文件访问：/api/files/** → lemon.upload.path */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(FILE_URL_PREFIX + "**")
                .addResourceLocations(uploadProperties.root().toUri().toString());
    }
}
