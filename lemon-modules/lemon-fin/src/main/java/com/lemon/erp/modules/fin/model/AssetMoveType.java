package com.lemon.erp.modules.fin.model;

/**
 * 资产变动类型，与字典 fin_asset_move_type 对应
 */
public enum AssetMoveType {
    /** 领用：闲置 → 使用中 */
    ASSIGN,
    /** 归还：使用中 → 闲置，清空使用人 */
    RETURN,
    /** 调拨：变更部门、使用人、存放地点 */
    TRANSFER
}
