package com.lemon.erp.modules.wms.model;

/**
 * 组装拆卸单种类（字典 wms_assembly_kind）
 */
public enum AssemblyKind {
    /** 组装：子件出库，成套件入库 */
    ASSEMBLE,
    /** 拆卸：成套件出库，子件入库 */
    DISASSEMBLE
}
