package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.entity.ScmPriceMemory;
import com.lemon.erp.modules.scm.model.OrderKind;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ScmPriceMemoryMapper extends BaseMapper<ScmPriceMemory> {

    /**
     * 登记一次成交（原子操作，并发确认也不会丢失更新）：没有记录时新建；
     * 有记录时日期不早于最新日期的更新最新价，低于最低价 / 高于最高价的更新最低 / 最高价及日期
     */
    int upsert(@Param("kind") OrderKind kind, @Param("partnerId") Long partnerId, @Param("materialId") Long materialId,
               @Param("date") LocalDate date, @Param("lastPrice") BigDecimal lastPrice,
               @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    /** 物料的价格记忆（供商 / 客户对比），按最新日期倒序 */
    List<PricingViews.MemoryRow> selectCompare(@Param("kind") OrderKind kind, @Param("q") PricingViews.ReferenceQuery q);
}
