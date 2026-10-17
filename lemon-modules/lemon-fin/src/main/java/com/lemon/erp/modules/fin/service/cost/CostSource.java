package com.lemon.erp.modules.fin.service.cost;

import com.lemon.erp.modules.fin.entity.FinCostLayer;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.service.cost.CostRows.Assembly;
import com.lemon.erp.modules.fin.service.cost.CostRows.AssemblyRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.ConsumeRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostEvent;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostMaterial;
import com.lemon.erp.modules.fin.service.cost.CostRows.IoCategoryMovement;
import com.lemon.erp.modules.fin.service.cost.CostRows.OutsourceConsumption;
import com.lemon.erp.modules.fin.service.cost.CostRows.QtyAmount;
import com.lemon.erp.modules.fin.service.cost.CostRows.ReportCost;
import com.lemon.erp.modules.fin.service.cost.CostRows.StockMovement;
import com.lemon.erp.modules.fin.service.cost.CostRows.StockSums;
import com.lemon.erp.modules.fin.service.cost.CostRows.TradeSum;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoDocQty;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoDocSum;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoReport;
import com.lemon.erp.modules.fin.service.cost.CostRows.WorkOrderInfo;
import com.lemon.erp.modules.mes.model.WoDocKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 一个期间成本核算的输入数据：上期结果与本期业务数据的汇总。由 {@link CostSourceLoader} 从数据库读取，
 * 单元测试中可以直接构造；按行加入时在这里归类汇总（与数据库无关）
 */
@Getter
public final class CostSource {

    private final int period;
    /** 物料 → 上期成本 */
    private final Map<Long, FinMaterialCost> prev = new HashMap<>();
    /** 工单 → 上期成本 */
    private final Map<Long, FinWoCost> prevWo = new HashMap<>();
    /** 上期没有核算时（首次核算），物料 → 本期之前全部流水的结存数量，期初金额按 0 */
    private final Map<Long, BigDecimal> openQtyFallback = new HashMap<>();
    /** 物料 → 参考成本价（大于 0） */
    private final Map<Long, BigDecimal> refCosts = new HashMap<>();
    /** 物料 → 本期库存流水合计 */
    private final Map<Long, StockSums> stock = new HashMap<>();
    /** 物料 → 种类 → 数量、不含税金额 */
    private final Map<Long, Map<TradeKind, QtyAmount>> trade = new HashMap<>();
    /** 工单 → 物料 → 领料、退料、完工 */
    private final Map<Long, Map<Long, WoDocQty>> woDocs = new HashMap<>();
    private final Map<Long, WoReport> woReports = new HashMap<>();
    private final Map<Long, WorkOrderInfo> workOrders = new HashMap<>();
    /** 物料 → 项目领料数量（扣除退回）、委外发料数量，计入合同履约成本、委托加工物资 */
    private final Map<Long, BigDecimal> projectOut = new HashMap<>();
    private final Map<Long, BigDecimal> outsourceOut = new HashMap<>();
    /** 服务工单领用的备件（售后），计入销售费用 */
    private final Map<Long, BigDecimal> serviceOut = new HashMap<>();
    /** 物料 → 入库类别 / 出库类别 → 数量：其他出入库按类别取对方科目 */
    private final Map<Long, Map<String, BigDecimal>> categoryIn = new HashMap<>();
    private final Map<Long, Map<String, BigDecimal>> categoryOut = new HashMap<>();
    /** 低值易耗品部门领用 */
    private final List<ConsumeRow> consumes = new ArrayList<>();
    /** 委外件 → 子件 → 收货核销的数量：子件成本从委托加工物资转入委外件 */
    private final Map<Long, Map<Long, BigDecimal>> outsourceConsume = new HashMap<>();
    /** 组装拆卸单号 → 单据（按单据顺序） */
    @Getter(lombok.AccessLevel.NONE)
    private final Map<String, Assembly> assemblies = new LinkedHashMap<>();
    /** 物料 → 组装拆卸的入库数量（组装的成套件、拆卸的子件）、出库数量 */
    private final Map<Long, BigDecimal> assemblyIn = new HashMap<>();
    private final Map<Long, BigDecimal> assemblyOut = new HashMap<>();
    /** 参与核算的物料（物料资料中存在的） */
    private final Map<Long, CostMaterial> materials = new LinkedHashMap<>();
    /** 移动加权平均、先进先出的物料 → 本期出入库明细（按发生顺序） */
    private final Map<Long, List<CostEvent>> events = new HashMap<>();
    /** 先进先出的物料 → 上期结存批次（按顺序） */
    private final Map<Long, List<FinCostLayer>> prevLayers = new HashMap<>();

    public CostSource(int period) {
        this.period = period;
    }

    /** 本期已完成的组装拆卸单 */
    public Collection<Assembly> getAssemblies() {
        return assemblies.values();
    }

    // ---------------------------------------------------------------- 按行加入

    public void addPrev(FinMaterialCost c) {
        prev.put(c.getMaterialId(), c);
    }

    public void addPrevWo(FinWoCost w) {
        prevWo.put(w.getWoId(), w);
    }

    /** 首次核算：本期之前的流水推算期初数量 */
    public void addOpeningMovement(StockMovement row) {
        openQtyFallback.merge(row.materialId(), row.inQty().subtract(row.outQty()), BigDecimal::add);
    }

    public void addRefCost(Long materialId, BigDecimal refCost) {
        refCosts.put(materialId, refCost);
    }

    /** 本期库存流水：合计入库、出库，并按业务类型归入销售、组装拆卸、项目领料、委外发料、售后备件 */
    public void addMovement(StockMovement row) {
        Long m = row.materialId();
        StockSums v = stock.getOrDefault(m, StockSums.ZERO);
        BigDecimal salesOut = v.salesOut();
        BigDecimal salesReturn = v.salesReturn();
        // 销售按仓库出库类型统计（含不经过销售订单、直接在仓库做的销售出库）
        if (row.is(StockOutType.SALES)) {
            salesOut = salesOut.add(row.outQty());
        } else if (row.is(StockInType.SALES_RETURN)) {
            salesReturn = salesReturn.add(row.inQty());
        } else if (row.is(StockInType.ASSEMBLY)) {
            assemblyIn.merge(m, row.inQty(), BigDecimal::add);
            assemblyOut.merge(m, row.outQty(), BigDecimal::add);
        } else if (row.is(StockOutType.PROJECT)) {
            projectOut.merge(m, row.outQty().subtract(row.inQty()), BigDecimal::add);
        } else if (row.is(StockOutType.OUTSOURCE, StockInType.OUTSOURCE_RETURN)) {
            // 委外发料计入委托加工物资，加工商退回（委外收回）冲减
            outsourceOut.merge(m, row.outQty().subtract(row.inQty()), BigDecimal::add);
        } else if (row.is(StockOutType.SERVICE)) {
            serviceOut.merge(m, row.outQty().subtract(row.inQty()), BigDecimal::add);
        }
        stock.put(m, new StockSums(v.inQty().add(row.inQty()), v.outQty().add(row.outQty()), salesOut, salesReturn));
    }

    public void addConsume(ConsumeRow row) {
        consumes.add(row);
    }

    public void addIoCategory(IoCategoryMovement row) {
        Map<Long, Map<String, BigDecimal>> target = row.kind() == StockBizType.IN ? categoryIn : categoryOut;
        target.computeIfAbsent(row.materialId(), k -> new HashMap<>()).merge(row.ioCategory(), row.qty(), BigDecimal::add);
    }

    public void addTrade(TradeSum row) {
        trade.computeIfAbsent(row.materialId(), k -> new HashMap<>()).put(row.kind(), new QtyAmount(row.qty(), row.net()));
    }

    public void addAssembly(AssemblyRow row) {
        Assembly a = assemblies.computeIfAbsent(row.assemblyNo(), no -> new Assembly(no, row.kind() == AssemblyKind.ASSEMBLE,
                row.productId(), row.qty(), new LinkedHashMap<>()));
        a.parts().merge(row.partId(), row.partQty(), BigDecimal::add);
    }

    public void addOutsourceConsumption(OutsourceConsumption row) {
        outsourceConsume.computeIfAbsent(row.productId(), k -> new HashMap<>()).merge(row.materialId(), row.qty(), BigDecimal::add);
    }

    /** 工单领料（含补料）、退料、完工；报废入库不是完工，按其他入库计价（贷记待处理财产损溢），不计入 */
    public void addWoDoc(WoDocSum row) {
        if (row.kind() == WoDocKind.SCRAP) {
            return;
        }
        Map<Long, WoDocQty> byMaterial = woDocs.computeIfAbsent(row.woId(), k -> new HashMap<>());
        WoDocQty v = byMaterial.getOrDefault(row.materialId(), WoDocQty.ZERO);
        byMaterial.put(row.materialId(), switch (row.kind()) {
            case ISSUE, SUPPLEMENT -> new WoDocQty(v.issue().add(row.qty()), v.returned(), v.finished());
            case RETURN -> new WoDocQty(v.issue(), v.returned().add(row.qty()), v.finished());
            default -> new WoDocQty(v.issue(), v.returned(), v.finished().add(row.qty()));
        });
    }

    public void addReport(ReportCost row) {
        woReports.put(row.woId(), row.report());
    }

    public void addWorkOrder(WorkOrderInfo wo) {
        workOrders.put(wo.id(), wo);
    }

    public void addMaterial(CostMaterial m) {
        materials.put(m.id(), m);
    }

    public void addEvent(CostEvent row) {
        events.computeIfAbsent(row.materialId(), k -> new ArrayList<>()).add(row);
    }

    public void addPrevLayer(FinCostLayer l) {
        prevLayers.computeIfAbsent(l.getMaterialId(), k -> new ArrayList<>()).add(l);
    }

    // ---------------------------------------------------------------- 取数中间步骤

    /** 需要读取基本信息的工单：本期有单据或报工，或上期有在产品 */
    public Set<Long> activeWoIds() {
        Set<Long> woIds = new HashSet<>(woDocs.keySet());
        woIds.addAll(woReports.keySet());
        prevWo.values().stream().filter(w -> w.getWipClose().signum() != 0).forEach(w -> woIds.add(w.getWoId()));
        return woIds;
    }

    /** 涉及的全部物料（按 ID 排序） */
    public Set<Long> involvedMaterialIds() {
        Set<Long> all = new TreeSet<>(prev.keySet());
        all.addAll(openQtyFallback.keySet());
        all.addAll(stock.keySet());
        all.addAll(trade.keySet());
        workOrders.values().forEach(w -> all.add(w.materialId()));
        woDocs.values().forEach(m -> all.addAll(m.keySet()));
        outsourceConsume.forEach((product, parts) -> {
            all.add(product);
            all.addAll(parts.keySet());
        });
        assemblies.values().forEach(a -> {
            all.add(a.product());
            all.addAll(a.parts().keySet());
        });
        return all;
    }
}
