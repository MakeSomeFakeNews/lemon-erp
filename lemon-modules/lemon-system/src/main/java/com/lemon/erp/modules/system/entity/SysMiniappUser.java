package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户与微信小程序 openid 的绑定：每个租户中一个微信只绑定一个用户，一个用户只绑定一个微信
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_miniapp_user")
public class SysMiniappUser extends BaseEntity {

    /** 租户：登录时还不知道租户，按 openid 跨租户查询 */
    private Long tenantId;

    private Long userId;

    private String openid;
}
