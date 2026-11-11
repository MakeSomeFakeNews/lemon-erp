package com.lemon.erp.modules.wms.service;

import java.util.List;

/**
 * 切换物料的批次 / 序列号管理前，检查其他模块中按已确定的批次、序列号执行、且不再按物料管理方式重新规范的进行中单据
 * （如销售发货通知的拣货明细）。依赖仓库模块的业务模块实现为 Bean；有返回时不允许切换
 */
public interface MaterialTrackingGuard {

    /**
     * @return 进行中单据的说明（如「发货通知 FH20261111001」，已按当前语言），没有返回空列表
     */
    List<String> openDocs(Long materialId);
}
