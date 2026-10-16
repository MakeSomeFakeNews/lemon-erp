package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.pm.entity.PmCost;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PmCostMapper extends BaseMapper<PmCost> {

    IPage<PmCost> selectPageList(IPage<PmCost> page, @Param("projectId") Long projectId, @Param("costType") String costType,
                                 @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /** 项目各成本类型的实际发生：人工取已审批工时，其余取费用 */
    List<Map<String, Object>> selectActualByType(@Param("projectId") Long projectId);

    /** 项目按月成本：人工、费用 */
    List<Map<String, Object>> selectMonthly(@Param("projectId") Long projectId);

    /**
     * 物料单位成本：最近一期成本核算的加权平均单价，没有时取采购入库的平均不含税单价
     *
     * @return [{materialId, unitCost}]
     */
    List<Map<String, Object>> selectUnitCosts(@Param("materialIds") Collection<Long> materialIds);
}
