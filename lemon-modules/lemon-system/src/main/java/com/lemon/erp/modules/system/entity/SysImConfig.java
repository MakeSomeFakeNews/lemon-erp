package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.system.im.ImPlatform;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 第三方集成（钉钉、企业微信、飞书）的应用配置，每个租户每个平台一条。
 * 密钥只允许写入：查询时不返回，保存时为空表示不修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_im_config")
public class SysImConfig extends BaseEntity {

    @NotNull(message = "{system.valid.platformRequired}")
    private ImPlatform platform;

    private Boolean enabled;

    /** 企业微信 corpid */
    @Size(max = 64, message = "{system.valid.corpIdCannotExceedCharacters}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String corpId;

    /** 钉钉 Client ID（AppKey）、飞书 App ID */
    @Size(max = 64, message = "{system.valid.appIdCannotExceedCharacters}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String appKey;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 128, message = "{system.valid.appSecretCannotExceedCharacters}")
    private String appSecret;

    /** 钉钉、企业微信应用的 AgentId */
    @Size(max = 32, message = "{system.valid.agentidCannotExceedCharacters}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String agentId;

    /** 系统访问地址：外网可访问的前端地址，如 https://erp.example.com */
    @Size(max = 255, message = "{system.valid.systemUrlCannotExceedCharacters}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String homeUrl;

    private Boolean loginEnabled;

    private Boolean messageEnabled;

    /** 同步通讯录时自动创建部门和账号（否则只按手机号绑定已有账号） */
    private Boolean syncCreate;

    @Size(max = 500, message = "{system.valid.webhookUrlCannotExceedCharacters}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String robotWebhook;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 128, message = "{system.valid.signingSecretCannotExceedCharacters}")
    private String robotSecret;

    /** 推送到群机器人的消息类别，逗号分隔（字典 sys_message_category） */
    @Size(max = 64, message = "{system.valid.tooManyMessageCategories}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String robotCategories;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String lastSyncResult;

    /** 是否已保存应用密钥（密钥本身不返回） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean appSecretSet;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean robotSecretSet;

    /** 在平台中登记的登录回调地址（填写系统访问地址后才有） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String callbackUrl;

    /** 在平台中登记的应用首页地址：平台客户端内打开时自动登录 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String entryUrl;

    /** 清空已保存的加签密钥（加签密钥可以不填，为空的输入表示不修改，需要单独标记清空） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @TableField(exist = false)
    private boolean clearRobotSecret;
}
