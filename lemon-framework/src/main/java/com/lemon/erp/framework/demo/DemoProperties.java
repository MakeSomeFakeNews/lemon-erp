package com.lemon.erp.framework.demo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示模式配置（lemon.demo）：开启后所有用户只能查看，不能新增、修改、删除
 */
@Data
@ConfigurationProperties(prefix = "lemon.demo")
public class DemoProperties {

    /** 是否开启演示模式 */
    private boolean enabled = false;

    /**
     * 演示模式下仍然放行的写请求（Ant 风格路径，不含 context-path）：登录退出、标记消息已读，
     * 以及用 POST 传参但不修改数据的查询（多维分析、促销预览、上架建议、单据打印）
     */
    private List<String> allowedPaths = new ArrayList<>(List.of(
            "/auth/login", "/auth/logout", "/system/message/read",
            "/bi/olap/pivot", "/scm/promotion/preview", "/wms/location/putaway", "/system/print/pdf", "/system/print/jrxml"));
}
