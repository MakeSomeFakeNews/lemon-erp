package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.TreeEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends TreeEntity<SysMenu> {

    public static final String TYPE_DIR = "M";
    public static final String TYPE_MENU = "C";
    public static final String TYPE_BUTTON = "F";

    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** M 目录 C 菜单 F 按钮 */
    @Pattern(regexp = "[MCF]", message = "菜单类型只能是 M/C/F")
    private String type;

    private String path;

    /** 前端组件路径，相对 src/views，例如 system/user/index */
    private String component;

    private String perms;

    private String icon;

    /** 1 显示 0 隐藏 */
    @Min(value = 0, message = "是否显示只能是 0 或 1")
    @Max(value = 1, message = "是否显示只能是 0 或 1")
    private Integer visible;

    /** 1 启用 0 停用 */
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
