package com.lemon.erp.modules.bi.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.bi.dto.JxcQuery;
import com.lemon.erp.modules.bi.dto.JxcViews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 进销存报表：实时汇总业务表（只读 JOIN scm、wms、fin 的表）。
 * sales = true 为销售出库 / 退货，false 为采购入库 / 退货；调用方按 scm_trade_doc 的制单人部门 + 制单人启用数据权限
 */
@Mapper
public interface BiJxcMapper {

    List<JxcViews.SummaryRow> selectSummary(@Param("sales") boolean sales, @Param("dim") String dim, @Param("q") JxcQuery q,
                                            @Param("limit") int limit);

    JxcViews.SummaryRow selectTotal(@Param("sales") boolean sales, @Param("q") JxcQuery q);

    Page<JxcViews.DetailRow> selectDetail(Page<JxcViews.DetailRow> page, @Param("sales") boolean sales, @Param("q") JxcQuery q);

    List<JxcViews.DetailRow> selectDetailList(@Param("sales") boolean sales, @Param("q") JxcQuery q, @Param("limit") int limit);

    /** 收款单 / 付款单（已生效）按月 */
    List<JxcViews.CashRow> selectSettlementByMonth(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 预收 / 预付款按月 */
    List<JxcViews.CashRow> selectAdvanceByMonth(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    List<JxcViews.InventoryRow> selectInventory();

    JxcViews.Balances selectBalances(@Param("today") LocalDate today);

    /** kind = AR（客户）/ AP（供应商） */
    List<JxcViews.PartnerBalance> selectTopBalances(@Param("kind") String kind, @Param("limit") int limit);

    Page<JxcViews.GoodsRow> selectGoodsTop(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    Page<JxcViews.GoodsRow> selectGoodsSlow(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    Page<JxcViews.GoodsRow> selectGoodsOverstock(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q, @Param("days") long days);

    Page<JxcViews.GoodsRow> selectGoodsShortage(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    Page<JxcViews.GoodsRow> selectGoodsTurnover(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    List<JxcViews.RfmRow> selectRfm(@Param("q") JxcQuery q);
}
