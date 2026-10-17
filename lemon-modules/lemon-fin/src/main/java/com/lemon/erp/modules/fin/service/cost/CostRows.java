package com.lemon.erp.modules.fin.service.cost;

import com.lemon.erp.modules.mes.model.WoDocKind;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.model.StockBizType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 成本核算的取数结果（{@link CostSourceLoader} 把查询结果转为这些强类型的行）与中间汇总值。
 * 数量、金额为空时按 0
 */
public final class CostRows {

    private CostRows() {
    }

    /** 数量与金额 */
    public record QtyAmount(BigDecimal qty, BigDecimal amount) {
        public static final QtyAmount ZERO = new QtyAmount(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** 物料本期的库存流水合计：入库、出库，其中销售出库、销售退货 */
    public record StockSums(BigDecimal inQty, BigDecimal outQty, BigDecimal salesOut, BigDecimal salesReturn) {
        public static final StockSums ZERO = new StockSums(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** 工单中某物料本期的领料（含补料）、退料、完工数量 */
    public record WoDocQty(BigDecimal issue, BigDecimal returned, BigDecimal finished) {
        public static final WoDocQty ZERO = new WoDocQty(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** 工单本期报工：工时、按费率计算的人工、制造费用 */
    public record WoReport(BigDecimal hours, BigDecimal labor, BigDecimal overhead) {
        public static final WoReport ZERO = new WoReport(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** 物料资料中成本核算用到的部分 */
    public record CostMaterial(Long id, String code, String name, String category, CostMethod costMethod) {
    }

    /**
     * 库存流水按物料、业务类型的汇总
     *
     * @param bizType 入库单为入库类型、出库单为出库类型的名称，盘点为 CHECK
     */
    public record StockMovement(Long materialId, String bizType, BigDecimal inQty, BigDecimal outQty) {
        public boolean is(Enum<?>... types) {
            return bizTypeIs(bizType, List.of(types));
        }
    }

    /** 填写了收发类别的出入库数量 */
    public record IoCategoryMovement(Long materialId, StockBizType kind, String ioCategory, BigDecimal qty) {
    }

    /** 采购入库 / 退货、销售出库 / 退货按物料、种类的数量和不含税金额 */
    public record TradeSum(Long materialId, TradeKind kind, BigDecimal qty, BigDecimal net) {
    }

    /** 工单单据按工单、物料、种类汇总的数量 */
    public record WoDocSum(Long woId, Long materialId, WoDocKind kind, BigDecimal qty) {
    }

    /** 工单报工汇总 */
    public record ReportCost(Long woId, WoReport report) {
    }

    /**
     * 工单基本信息
     *
     * @param finishedBefore 期初之前累计完工数量
     * @param closeDate      关闭日期（未关闭为空）
     */
    public record WorkOrderInfo(Long id, Long materialId, BigDecimal qty, BigDecimal finishedBefore, WorkOrderStatus status,
                                LocalDate closeDate) {
    }

    /** 委外收货核销的用料：委外件、子件、数量 */
    public record OutsourceConsumption(Long productId, Long materialId, BigDecimal qty) {
    }

    /** 组装拆卸单的一行子件 */
    public record AssemblyRow(String assemblyNo, AssemblyKind kind, Long productId, BigDecimal qty, Long partId, BigDecimal partQty) {
    }

    /** 低值易耗品部门领用：物料、部门、费用科目、数量 */
    public record ConsumeRow(Long materialId, Long deptId, Long accountId, BigDecimal qty) {
    }

    /**
     * 移动加权平均、先进先出物料的一笔出入库（按单据 + 物料汇总）
     *
     * @param bizType  同 {@link StockMovement#bizType()}
     * @param docKind  出入库单的种类，盘点为空
     * @param sourceNo 来源单号（组装拆卸单号）
     * @param qty      入库为正、出库为负（红字单相反）
     * @param tradeQty 采购入库 / 退货单据中该物料的数量
     * @param tradeNet 采购入库 / 退货单据中该物料的不含税金额，不是采购单据时为空
     * @param woId     完工入库的工单
     */
    public record CostEvent(Long materialId, LocalDate bizDate, String bizType, StockBizType docKind, String sourceNo,
                            BigDecimal qty, BigDecimal tradeQty, BigDecimal tradeNet, Long woId) {
        public boolean is(Enum<?>... types) {
            return bizTypeIs(bizType, List.of(types));
        }

        public boolean isAny(Collection<? extends Enum<?>> types) {
            return bizTypeIs(bizType, types);
        }

        /** 是否带有采购单据的金额 */
        public boolean hasTrade() {
            return tradeNet != null;
        }
    }

    /** 一张组装拆卸单：组装（assemble）时子件出库、成套件入库，拆卸相反；parts 按单据明细顺序 */
    public record Assembly(String no, boolean assemble, Long product, BigDecimal qty, Map<Long, BigDecimal> parts) {
    }

    /**
     * 出入库流水的业务类型是否为其中之一。
     * 同一流水查询中入库、出库类型混在一起（如生产领料与生产入库同名），只能按名称比较
     */
    static boolean bizTypeIs(String bizType, Collection<? extends Enum<?>> types) {
        for (Enum<?> type : types) {
            if (type.name().equals(bizType)) {
                return true;
            }
        }
        return false;
    }
}
