package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.TreeEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.system.model.MenuType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends TreeEntity<SysMenu> {

    @NotBlank(message = "{system.valid.menuNameRequired}")
    private String name;

    private MenuType type;

    private String path;

    /** 前端组件路径，相对 src/views，例如 system/user/index */
    private String component;

    private String perms;

    private String icon;

    /** 1 显示 0 隐藏 */
    @Min(value = 0, message = "{system.valid.visibleMust}")
    @Max(value = 1, message = "{system.valid.visibleMust}")
    private Integer visible;

    /** 1 启用 0 停用 */
    private EnableStatus status;
}
