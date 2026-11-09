package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsMaterialCost;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WmsMaterialCostMapper extends BaseMapper<WmsMaterialCost> {

    /** 按物料 ID 顺序加行锁读取成本（并发确认时按相同顺序加锁，避免死锁） */
    List<WmsMaterialCost> selectForUpdate(@Param("materialIds") Collection<Long> materialIds);

    /**
     * 各物料计价的库存数量合计（不含供应商寄售仓），初始化成本时使用
     *
     * @return [{materialId, qty}]
     */
    List<Map<String, Object>> selectValuedQty(@Param("materialIds") Collection<Long> materialIds);

    void updateCost(@Param("id") Long id, @Param("qty") BigDecimal qty, @Param("amount") BigDecimal amount,
                    @Param("unitCost") BigDecimal unitCost);
}
