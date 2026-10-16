package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesForecast;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MesForecastMapper extends BaseMapper<MesForecast> {

    IPage<MesForecast> selectPageList(IPage<MesForecast> page, @Param("beginPeriod") Integer beginPeriod,
                                      @Param("endPeriod") Integer endPeriod, @Param("keyword") String keyword);

    /**
     * MRP：期间内的预测，扣减同一月份已审核未完成的销售订单的未出库数量（预测消耗）
     *
     * @return [{materialId, period, qty}]，只返回净值大于 0 的
     */
    List<Map<String, Object>> selectNetForecast(@Param("beginPeriod") int beginPeriod, @Param("endPeriod") int endPeriod);

    /**
     * MRP：已审核未完成的销售订单中还没有生成生产计划的数量（未出库 - 已生成计划）
     *
     * @return [{materialId, dueDate, qty}]
     */
    List<Map<String, Object>> selectUnplannedSales();
}
