package com.lemon.erp.modules.bi.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.bi.dto.JxcQuery;
import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.dto.OverstockQuery;
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

    /** 单据列表：按出入库单汇总符合条件的明细行 */
    Page<JxcViews.DocRow> selectDocs(Page<JxcViews.DocRow> page, @Param("sales") boolean sales, @Param("q") JxcQuery q);

    List<JxcViews.DocRow> selectDocList(@Param("sales") boolean sales, @Param("q") JxcQuery q, @Param("limit") int limit);

    /** 收款单 / 付款单（已生效，不含票据业务生成的）按月 */
    List<JxcViews.CashRow> selectSettlementByMonth(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 预收 / 预付款按月 */
    List<JxcViews.CashRow> selectAdvanceByMonth(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 票据托收、贴现（收款）与应付票据兑付（付款）按月 */
    List<JxcViews.CashRow> selectNoteCashByMonth(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 截至 asOf（含）的库存数量与金额，按仓库、物料分类 */
    List<JxcViews.InventoryRow> selectInventory(@Param("asOf") LocalDate asOf);

    /** 截至 asOf（含）的应收应付未核销余额（逾期 = 到期日早于 asOf）、预收预付余额、应收应付票据 */
    JxcViews.Balances selectBalances(@Param("asOf") LocalDate asOf);

    /** 截至 asOf 已生效、还没有生成凭证的收付款（不含票据业务） */
    JxcViews.Unvouchered selectUnvouchered(@Param("asOf") LocalDate asOf);

    /** 截至 asOf 的往来余额排行，kind = AR（客户）/ AP（供应商） */
    List<JxcViews.PartnerBalance> selectTopBalances(@Param("kind") String kind, @Param("asOf") LocalDate asOf, @Param("limit") int limit);

    Page<JxcViews.GoodsRow> selectGoodsTop(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    Page<JxcViews.GoodsRow> selectGoodsSlow(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    Page<JxcViews.GoodsRow> selectGoodsOverstock(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q, @Param("days") long days);

    Page<JxcViews.GoodsRow> selectGoodsShortage(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    Page<JxcViews.GoodsRow> selectGoodsTurnover(Page<JxcViews.GoodsRow> page, @Param("q") JxcQuery q);

    List<JxcViews.RfmRow> selectRfm(@Param("q") JxcQuery q);

    /** 积压分析（issue #111）：积压数量大于 0 的物料，有成本权限时按资金占用排序 */
    Page<JxcViews.OverstockRow> selectOverstock(Page<JxcViews.OverstockRow> page, @Param("q") OverstockQuery q);

    /** 积压分析的合计（全部积压物料） */
    JxcViews.OverstockRow selectOverstockTotal(@Param("q") OverstockQuery q);
}
