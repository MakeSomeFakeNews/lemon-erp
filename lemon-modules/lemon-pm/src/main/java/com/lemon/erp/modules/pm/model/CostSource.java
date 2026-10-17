package com.lemon.erp.modules.pm.model;

/**
 * 项目费用来源
 */
public enum CostSource {
    /** 手工录入（发布了项目费用审批流程时需审批） */
    MANUAL,
    /** 项目领料生成 */
    ISSUE
}
