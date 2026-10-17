package com.lemon.erp.modules.system.im;

import com.lemon.erp.modules.system.entity.SysImConfig;

import java.util.List;

/**
 * 一个平台的开放接口：授权登录、应用消息、通讯录、群机器人。接口出错时抛出 BizException（消息带平台名称和错误原因）
 */
public interface ImClient {

    ImPlatform platform();

    /** 校验应用凭证（重新获取 access_token） */
    void verify(SysImConfig config);

    /**
     * 授权页地址
     *
     * @param inApp 在平台客户端内打开（静默授权），否则为浏览器扫码登录
     */
    String authorizeUrl(SysImConfig config, String redirectUri, String state, boolean inApp);

    /** 用授权码换取成员 */
    ImIdentity identify(SysImConfig config, String code, String redirectUri);

    /** 以应用身份给成员发送消息 */
    void send(SysImConfig config, List<String> userIds, ImCard card);

    /** 应用可见范围内的部门和成员 */
    ImContacts contacts(SysImConfig config);

    /** 发送到群机器人 */
    void robot(String webhook, String secret, ImCard card);

    /** 群机器人 Webhook 地址的前缀，配置时校验 */
    String robotPrefix();
}
