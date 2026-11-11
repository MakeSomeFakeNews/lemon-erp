package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印模板：租户修改过的单据打印配置（JSON，结构由前端 utils/printTemplates.js 定义），未修改的单据使用默认版式；
 * 上传了 JRXML 的单据按 JRXML 打印（见 print/PrintService）。
 * 同一模板标识可以有多份打印格式（每行一份），其中有且只有一份默认格式；有多份启用的格式时打印前选择
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_template")
public class SysPrintTemplate extends BaseEntity {

    /** 模板标识，如 scm.purchaseOrder；_common 为通用设置（公司名称等） */
    private String templateKey;

    /** 格式名称，同一模板标识内唯一；只有一份格式时为「标准格式」 */
    private String name;

    /** 是否默认格式：每个模板标识只有一份 */
    private Boolean isDefault;

    /** 打印时选择格式的顺序 */
    private Integer sort;

    /** 默认格式不能停用 */
    private EnableStatus status;

    private String config;

    /** 自定义 JRXML（Jaspersoft Studio 7），内容较大，列表查询不返回 */
    @TableField(select = false)
    private String jrxml;

    /** 套打底图（data URL），只在设计时对照位置；列表查询不读取 */
    @TableField(select = false)
    private String background;
}
