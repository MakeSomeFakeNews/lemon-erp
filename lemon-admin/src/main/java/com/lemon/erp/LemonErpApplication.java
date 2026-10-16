package com.lemon.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.lemon.erp.modules.*.mapper")
public class LemonErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(LemonErpApplication.class, args);
    }
}
