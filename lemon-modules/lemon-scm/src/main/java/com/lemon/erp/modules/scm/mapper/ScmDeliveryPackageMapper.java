package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPackage;
import com.lemon.erp.modules.scm.entity.ScmDeliveryPackageItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ScmDeliveryPackageMapper extends BaseMapper<ScmDeliveryPackage> {

    /** 发货通知的装箱明细（按箱序号） */
    List<ScmDeliveryPackageItem> selectItems(@Param("deliveryId") Long deliveryId);

    int insertItems(@Param("packageId") Long packageId, @Param("list") List<ScmDeliveryPackageItem> items);

    int deleteItems(@Param("packageId") Long packageId);

    /** 各通知明细的已装箱数量（不含 excludePackageId）：deliveryItemId、qty */
    List<Map<String, Object>> selectPackedQty(@Param("deliveryId") Long deliveryId, @Param("excludePackageId") Long excludePackageId);
}
