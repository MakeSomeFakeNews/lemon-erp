package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.model.SettleKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.dto.PartnerAmountRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinSettlementMapper extends BaseMapper<FinSettlement> {

    /**
     * @param keyword      单号
     * @param discountOnly 只查有折让的收付款（折让记录）
     */
    IPage<FinSettlement> selectPageList(IPage<FinSettlement> page, @Param("kind") SettleKind kind,
                                        @Param("partnerId") Long partnerId, @Param("keyword") String keyword,
                                        @Param("discountOnly") boolean discountOnly);

    FinSettlement selectDetail(@Param("id") Long id);

    /** 已生效的收款 / 付款按往来单位汇总（日期范围），金额降序；keyword 匹配往来单位编码或名称 */
    List<PartnerAmountRow> selectPartnerSummary(@Param("kind") SettleKind kind, @Param("begin") LocalDate begin,
                                                @Param("end") LocalDate end, @Param("keyword") String keyword);
}
