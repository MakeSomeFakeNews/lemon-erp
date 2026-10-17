package com.lemon.erp.modules.system.im;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 钉钉、企业微信、飞书开放平台的接口地址。一般不需要修改；私有化部署的飞书（或 Lark 国际版）可改为对应地址
 */
@Data
@ConfigurationProperties(prefix = "lemon.im")
public class ImProperties {

    private DingTalk dingtalk = new DingTalk();
    private WeCom wecom = new WeCom();
    private Feishu feishu = new Feishu();

    @Data
    public static class DingTalk {
        /** 旧版服务端接口（access_token、工作通知、通讯录） */
        private String oapi = "https://oapi.dingtalk.com";
        /** 新版服务端接口（用户授权） */
        private String api = "https://api.dingtalk.com";
        /** 登录授权页 */
        private String login = "https://login.dingtalk.com";
    }

    @Data
    public static class WeCom {
        private String api = "https://qyapi.weixin.qq.com";
        /** 企业微信客户端内的网页授权 */
        private String open = "https://open.weixin.qq.com";
        /** 浏览器中的扫码登录 */
        private String login = "https://login.work.weixin.qq.com";
    }

    @Data
    public static class Feishu {
        private String api = "https://open.feishu.cn";
        private String accounts = "https://accounts.feishu.cn";
    }
}
