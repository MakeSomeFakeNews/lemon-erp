package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinStdCost;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FinStdCostMapper extends BaseMapper<FinStdCost> {

    IPage<FinStdCost> selectPageList(IPage<FinStdCost> page, @Param("versionId") Long versionId, @Param("keyword") String keyword,
                                     @Param("make") Integer make);

    int insertBatch(@Param("items") List<FinStdCost> items);

    /** 启用的默认 BOM：bomId、materialId */
    List<Map<String, Object>> selectDefaultBoms();

    /** BOM 子件：bomId、materialId、qty、lossRate */
    List<Map<String, Object>> selectBomItems(@Param("bomIds") Collection<Long> bomIds);

    /** BOM 工序：bomId、seq、name、workCenterName、stdHours、laborRate、overheadRate */
    List<Map<String, Object>> selectBomOps(@Param("bomIds") Collection<Long> bomIds);

    /** 采购件的初始单价：最近一次成本核算的单价，没有时取参考成本价：materialId、price */
    List<Map<String, Object>> selectInitialPrices(@Param("ids") Collection<Long> ids);
}
