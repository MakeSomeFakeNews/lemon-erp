package com.lemon.erp.modules.fin.service.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.fin.entity.FinCostLayer;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.mapper.FinConsumableUseMapper;
import com.lemon.erp.modules.fin.mapper.FinCostLayerMapper;
import com.lemon.erp.modules.fin.mapper.FinCostSourceMapper;
import com.lemon.erp.modules.fin.mapper.FinMaterialCostMapper;
import com.lemon.erp.modules.fin.mapper.FinWoCostMapper;
import com.lemon.erp.modules.fin.service.cost.CostRows.AssemblyRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.ConsumeRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostEvent;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostMaterial;
import com.lemon.erp.modules.fin.service.cost.CostRows.IoCategoryMovement;
import com.lemon.erp.modules.fin.service.cost.CostRows.OutsourceConsumption;
import com.lemon.erp.modules.fin.service.cost.CostRows.ReportCost;
import com.lemon.erp.modules.fin.service.cost.CostRows.StockMovement;
import com.lemon.erp.modules.fin.service.cost.CostRows.TradeSum;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoDocSum;
import com.lemon.erp.modules.fin.service.cost.CostRows.WoReport;
import com.lemon.erp.modules.fin.service.cost.CostRows.WorkOrderInfo;
import com.lemon.erp.modules.mes.model.WoDocKind;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.firstDay;
import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.previous;

/**
 * 读取一个期间成本核算的输入数据（{@link CostSource}）：上期成本结果、仓库流水、采购销售、工单、组装拆卸、委外核销、低值易耗品领用。
 * 数量取仓库流水（按单据业务日期归属期间，不含调拨），销售按仓库出库类型统计；采购金额取采购入库 / 退货的不含税金额。
 * 查询结果（Map）在这里转为 {@link CostRows} 中的强类型行
 */
@Component
@RequiredArgsConstructor
public class CostSourceLoader {

    private final FinCostSourceMapper sourceMapper;
    private final FinMaterialCostMapper materialCostMapper;
    private final FinWoCostMapper woCostMapper;
    private final FinCostLayerMapper layerMapper;
    private final FinConsumableUseMapper consumableMapper;
    private final WmsMaterialService materialService;

    public CostSource load(int period) {
        LocalDate begin = firstDay(period);
        LocalDate end = lastDay(period);
        CostSource src = new CostSource(period);
        for (Map<String, Object> row : sourceMapper.selectRefCosts()) {
            src.addRefCost(id(row, "materialId"), dec(row, "refCost"));
        }
        int prevPeriod = previous(period);
        materialCostMapper.selectList(Wrappers.<FinMaterialCost>lambdaQuery().eq(FinMaterialCost::getPeriod, prevPeriod))
                .forEach(src::addPrev);
        woCostMapper.selectList(Wrappers.<FinWoCost>lambdaQuery().eq(FinWoCost::getPeriod, prevPeriod))
                .forEach(src::addPrevWo);
        // 上期没有核算：期初数量取本期之前的全部流水，金额按 0（首次核算）
        if (src.getPrev().isEmpty()) {
            for (Map<String, Object> row : sourceMapper.selectStockMovements(null, begin.minusDays(1))) {
                src.addOpeningMovement(stockMovement(row));
            }
        }
        for (Map<String, Object> row : sourceMapper.selectStockMovements(begin, end)) {
            src.addMovement(stockMovement(row));
        }
        for (Map<String, Object> row : consumableMapper.selectForCost(begin, end)) {
            src.addConsume(new ConsumeRow(id(row, "materialId"), idOrNull(row, "deptId"), idOrNull(row, "accountId"), dec(row, "qty")));
        }
        for (Map<String, Object> row : sourceMapper.selectIoCategoryMovements(begin, end)) {
            src.addIoCategory(new IoCategoryMovement(id(row, "materialId"), enumOf(StockBizType.class, row.get("kind")),
                    (String) row.get("ioCategory"), dec(row, "qty")));
        }
        for (Map<String, Object> row : sourceMapper.selectTradeSums(begin, end)) {
            src.addTrade(new TradeSum(id(row, "materialId"), enumOf(TradeKind.class, row.get("kind")), dec(row, "qty"), dec(row, "net")));
        }
        for (Map<String, Object> row : sourceMapper.selectAssemblies(begin, end)) {
            src.addAssembly(new AssemblyRow((String) row.get("assemblyNo"), enumOf(AssemblyKind.class, row.get("kind")),
                    id(row, "productId"), dec(row, "qty"), id(row, "partId"), dec(row, "partQty")));
        }
        for (Map<String, Object> row : sourceMapper.selectOutsourceConsumption(begin, end)) {
            src.addOutsourceConsumption(new OutsourceConsumption(id(row, "productId"), id(row, "materialId"), dec(row, "qty")));
        }
        for (Map<String, Object> row : sourceMapper.selectWoDocSums(begin, end)) {
            src.addWoDoc(new WoDocSum(id(row, "woId"), id(row, "materialId"), enumOf(WoDocKind.class, row.get("kind")), dec(row, "qty")));
        }
        for (Map<String, Object> row : sourceMapper.selectReportCosts(begin, end)) {
            src.addReport(new ReportCost(id(row, "woId"), new WoReport(dec(row, "hours"), dec(row, "labor"), dec(row, "overhead"))));
        }
        Set<Long> woIds = src.activeWoIds();
        if (!woIds.isEmpty()) {
            for (Map<String, Object> row : sourceMapper.selectWorkOrders(new ArrayList<>(woIds), begin)) {
                Object closeTime = row.get("closeTime");
                src.addWorkOrder(new WorkOrderInfo(id(row, "id"), id(row, "materialId"), dec(row, "qty"), dec(row, "finishedBefore"),
                        enumOf(WorkOrderStatus.class, row.get("status")), closeTime == null ? null : toLocalDate(closeTime)));
            }
        }
        for (WmsMaterial m : materialService.mapByIds(src.involvedMaterialIds()).values()) {
            src.addMaterial(new CostMaterial(m.getId(), m.getCode(), m.getName(), m.getCategory(), m.getCostMethod()));
        }
        Set<Long> sequenced = src.getMaterials().values().stream()
                .filter(m -> m.costMethod() != null && m.costMethod() != CostMethod.WEIGHTED)
                .map(CostMaterial::id).collect(Collectors.toSet());
        if (!sequenced.isEmpty()) {
            for (Map<String, Object> row : sourceMapper.selectCostEvents(begin, end, sequenced)) {
                Object tradeNet = row.get("tradeNet");
                Object bizDate = row.get("bizDate");
                src.addEvent(new CostEvent(id(row, "materialId"), bizDate == null ? null : toLocalDate(bizDate), (String) row.get("bizType"),
                        enumOf(StockBizType.class, row.get("docKind")), (String) row.get("sourceNo"), dec(row, "qty"),
                        dec(row, "tradeQty"), tradeNet == null ? null : dec(row, "tradeNet"), idOrNull(row, "woId")));
            }
            layerMapper.selectList(Wrappers.<FinCostLayer>lambdaQuery().eq(FinCostLayer::getPeriod, prevPeriod)
                            .in(FinCostLayer::getMaterialId, sequenced).orderByAsc(FinCostLayer::getSeq))
                    .forEach(src::addPrevLayer);
        }
        return src;
    }

    private static StockMovement stockMovement(Map<String, Object> row) {
        return new StockMovement(id(row, "materialId"), (String) row.get("bizType"), dec(row, "inQty"), dec(row, "outQty"));
    }

    private static LocalDate toLocalDate(Object v) {
        if (v instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        if (v instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }

    /** 查询结果（Map）中的取值转为枚举，为空时返回 null */
    private static <E extends Enum<E>> E enumOf(Class<E> type, Object value) {
        return value == null ? null : Enum.valueOf(type, value.toString());
    }

    private static Long id(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    private static Long idOrNull(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : ((Number) v).longValue();
    }

    private static BigDecimal dec(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
