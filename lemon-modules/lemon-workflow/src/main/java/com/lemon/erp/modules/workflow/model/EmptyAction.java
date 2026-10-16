package com.lemon.erp.modules.workflow.model;

/**
 * 审批人为空（如角色下没有启用的用户）时的处理方式
 */
public enum EmptyAction {

    /** 自动通过，跳过该节点 */
    AUTO_PASS,
    /** 转交给超级管理员审批 */
    TO_ADMIN
}
