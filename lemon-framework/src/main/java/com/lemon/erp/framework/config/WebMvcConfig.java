package com.lemon.erp.framework.config;

import com.lemon.erp.common.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
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

    /**
     * 查询参数 / 路径变量中的启用状态按 1 / 0 转换（默认的枚举转换器按常量名），非法值由
     * GlobalExceptionHandler 按参数格式错误提示
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, EnableStatus.class, EnableStatus::fromJson);
    }
}
