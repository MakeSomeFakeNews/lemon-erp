package com.lemon.erp.modules.wms.service;

/**
 * 切换物料的「虚拟商品」标识前，检查其他模块中会因切换而口径不一致的业务数据。依赖仓库模块的业务模块实现为 Bean；
 * 有返回时不允许切换（仓库模块自己检查库存业务，见 {@link WmsMaterialService}）
 */
public interface MaterialVirtualGuard {

    /**
     * @param toVirtual true：实物改为虚拟商品（如已用于采购订单、BOM、工单时不允许）；false：虚拟商品改为实物（如已有销售出库 / 退货时不允许，
     *                  这些单据没有库存流水和出库成本，改为实物后退货入库、报表成本都会不一致）
     * @return 阻止切换的业务说明（已按当前语言，如「采购订单」），没有返回 null
     */
    String findBlocker(Long materialId, boolean toVirtual);
}
