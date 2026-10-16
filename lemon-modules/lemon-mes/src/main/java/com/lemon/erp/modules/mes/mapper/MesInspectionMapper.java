package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.dto.InspectionQuery;
import com.lemon.erp.modules.mes.dto.QualitySummary;
import com.lemon.erp.modules.mes.entity.MesInspection;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesInspectionMapper extends BaseMapper<MesInspection> {

    IPage<MesInspection> selectPageList(IPage<MesInspection> page, @Param("q") InspectionQuery query);

    /** 按检验类型汇总 */
    List<QualitySummary> selectSummaryByType(@Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /** 来料检验按供应商汇总 */
    List<QualitySummary> selectSummaryBySupplier(@Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /**
     * 已确认的来料检验按供应商统计
     *
     * @return [{supplierId, batches, passBatches}]
     */
    List<Map<String, Object>> selectIqcStats(@Param("supplierIds") Collection<Long> supplierIds,
                                             @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /** 工单已确认的成品检验合格数量（合格、让步接收的送检数量合计） */
    BigDecimal sumFqcPassed(@Param("woId") Long woId);
}
