package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ScmSupplierPriceMapper extends BaseMapper<ScmSupplierPrice> {

    /**
     * @param keyword 物料编码或名称
     */
    IPage<ScmSupplierPrice> selectPageList(IPage<ScmSupplierPrice> page, @Param("supplierId") Long supplierId,
                                           @Param("materialId") Long materialId, @Param("keyword") String keyword);

    /** 物料的默认供应商价格（MRP 生成采购订单） */
    List<ScmSupplierPrice> selectDefaults(@Param("materialIds") Collection<Long> materialIds);

    /**
     * 采购价格历史：采购入库的含税单价（最近 100 条）
     *
     * @return [{bizDate, docNo, supplierId, supplierName, materialId, qty, price, taxRate}]
     */
    List<Map<String, Object>> selectPurchaseHistory(@Param("materialId") Long materialId, @Param("supplierId") Long supplierId);
}
