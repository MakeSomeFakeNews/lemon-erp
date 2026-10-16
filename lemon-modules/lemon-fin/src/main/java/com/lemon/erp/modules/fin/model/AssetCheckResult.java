package com.lemon.erp.modules.fin.model;

/**
 * 资产盘点结果，与字典 fin_asset_check_result 对应
 */
public enum AssetCheckResult {
    PENDING,
    /** 相符 */
    NORMAL,
    /** 位置变化：完成时更新卡片的部门、使用人、存放地点 */
    MOVED,
    /** 盘亏：完成时处置 */
    LOSS,
    /** 盘盈：账外资产，完成后另行登记卡片 */
    GAIN
}
