package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印模板：租户修改过的单据打印配置（JSON，结构由前端 utils/printTemplates.js 定义），未修改的单据使用默认版式；
 * 上传了 JRXML 的单据按 JRXML 打印（见 print/PrintService）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_print_template")
public class SysPrintTemplate extends BaseEntity {

    /** 模板标识，如 scm.purchaseOrder；_common 为通用设置（公司名称等） */
    private String templateKey;

    private String config;

    /** 自定义 JRXML（Jaspersoft Studio 7），内容较大，列表查询不返回 */
    @TableField(select = false)
    private String jrxml;
}
