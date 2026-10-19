package com.lemon.erp.modules.system.miniapp;

import com.lemon.erp.framework.message.MessageCategory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 微信小程序（lemon-mp）。一个小程序只有一个 AppID，SaaS 模式下所有租户共用，所以在配置文件中设置而不是按租户配置
 */
@Data
@ConfigurationProperties(prefix = "lemon.miniapp")
public class MiniappProperties {

    /** 小程序 AppID，为空时不能用微信登录 */
    private String appId;

    /** 小程序 AppSecret */
    private String appSecret;

    /** 微信服务端接口地址（测试时可改为本地模拟服务） */
    private String api = "https://api.weixin.qq.com";

    /** 订阅消息：站内消息同时推送到微信（审批提醒） */
    private Subscribe subscribe = new Subscribe();

    public boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }

    @Data
    public static class Subscribe {
        /** 订阅消息模板 ID（微信公众平台「订阅消息」中选用的模板），为空时不推送 */
        private String templateId;
        /** 模板中标题、内容、时间对应的字段名（模板详情中的 thing1、time3 等） */
        private String titleField = "thing1";
        private String contentField = "thing2";
        private String timeField = "time3";
        /** 推送的消息类别 */
        private List<MessageCategory> categories = List.of(MessageCategory.APPROVAL);
        /** 点击消息打开的页面 */
        private String page = "pages/approval/index";
        /** 打开的小程序版本：formal 正式版、trial 体验版、developer 开发版 */
        private String state = "formal";
    }
}
