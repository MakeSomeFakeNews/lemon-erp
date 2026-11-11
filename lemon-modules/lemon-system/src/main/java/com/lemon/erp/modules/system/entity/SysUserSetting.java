package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户的个人设置（每个用户每项一行）：如界面设置 ui（主题、布局等），内容为 JSON 对象，只能本人读写
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_setting")
public class SysUserSetting extends BaseEntity {

    private Long userId;

    /** 设置项标识，如 ui */
    private String settingKey;

    /** 设置内容（JSON 对象） */
    private String settingValue;
}
