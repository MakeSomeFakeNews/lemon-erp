package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.system.im.ImPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户与钉钉 / 企业微信 / 飞书成员的绑定：一个用户在每个平台只绑定一个成员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_im_user")
public class SysImUser extends BaseEntity {

    private Long userId;

    private ImPlatform platform;

    private String imUserId;

    private String imName;
}
