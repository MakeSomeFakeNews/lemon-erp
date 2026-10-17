package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.scm.dto.AtpResult;
import com.lemon.erp.modules.scm.dto.BackorderRow;
import com.lemon.erp.modules.scm.mapper.ScmPlanningMapper;
import com.lemon.erp.modules.scm.model.AtpLine;
import com.lemon.erp.modules.scm.model.AtpProvider;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.wms.service.StockReservationService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 可承诺量（ATP）与欠交清单
 */
@Service
@RequiredArgsConstructor
public class ScmPlanningService {

    private final ScmPlanningMapper planningMapper;
    private final WmsStockQueryService stockQueryService;
    private final StockReservationService reservationService;
    private final WmsMaterialService materialService;
    private final List<AtpProvider> providers;

    /**
     * 可承诺量：从空闲库存（可用 - 预留）出发，按日期累加采购在途、在制工单产出，扣减未预留的销售待发、工单待领料。
     * 现在可承诺的数量是未来各时点累计余额的最小值；传入数量时给出最早可承诺日期
     */
    public AtpResult atp(Long materialId, Long warehouseId, BigDecimal qty) {
        materialService.get(materialId);
        BigDecimal available;
        BigDecimal reserved;
        if (warehouseId != null) {
            BigDecimal free = reservationService.free(warehouseId, List.of(materialId)).getOrDefault(materialId, BigDecimal.ZERO);
            reserved = reservationService.reserved(warehouseId, materialId);
            available = free.add(reserved);
        } else {
            available = stockQueryService.available(List.of(materialId)).getOrDefault(materialId, BigDecimal.ZERO);
            reserved = reservationService.reserved(null, materialId);
        }
        BigDecimal free = available.subtract(reserved);
        LocalDate today = LocalDate.now();

        List<AtpLine> lines = new ArrayList<>();
        for (Map<String, Object> row : planningMapper.selectAtpLines(materialId, warehouseId, today)) {
            lines.add(new AtpLine(toDate(row.get("date")), (String) row.get("type"), (String) row.get("docNo"),
                    new BigDecimal(row.get("qty").toString())));
        }
        providers.forEach(p -> lines.addAll(p.lines(materialId, warehouseId)));
        // 同一天先计供应再计需求；过去的日期按今天
        lines.sort(Comparator.comparing((AtpLine l) -> l.date() == null || l.date().isBefore(today) ? today : l.date())
                .thenComparing(l -> l.qty().signum() < 0));

        List<AtpResult.Line> result = new ArrayList<>(lines.size());
        BigDecimal balance = free;
        for (AtpLine l : lines) {
            if (l.qty().signum() == 0) {
                continue;
            }
            balance = balance.add(l.qty());
            LocalDate d = l.date() == null || l.date().isBefore(today) ? today : l.date();
            result.add(new AtpResult.Line(d, l.type(), l.docNo(), l.qty(), balance));
        }
        // 从后往前求「此后累计余额的最小值」：在某日承诺的数量不能超过该值
        BigDecimal[] minAfter = new BigDecimal[result.size() + 1];
        minAfter[result.size()] = balance;
        for (int i = result.size() - 1; i >= 0; i--) {
            minAfter[i] = minAfter[i + 1].min(result.get(i).balance());
        }
        BigDecimal atp = free.min(minAfter[0]).max(BigDecimal.ZERO);
        LocalDate promiseDate = null;
        if (qty != null && qty.signum() > 0) {
            if (atp.compareTo(qty) >= 0) {
                promiseDate = today;
            } else {
                for (int i = 0; i < result.size(); i++) {
                    if (minAfter[i].compareTo(qty) >= 0 && result.get(i).qty().signum() > 0) {
                        promiseDate = result.get(i).date();
                        break;
                    }
                }
            }
        }
        return new AtpResult(available, reserved, free, atp, promiseDate, result);
    }

    /** 欠交清单：销售订单为客户欠交，采购订单为供应商欠交 */
    @DataPermission(table = "scm_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<BackorderRow> backorders(long current, long size, OrderKind kind, boolean overdueOnly, String keyword,
                                               Long partnerId) {
        Page<BackorderRow> page = Page.of(current, size);
        planningMapper.selectBackorders(page, kind, overdueOnly, keyword, partnerId, LocalDate.now());
        return PageResult.of(page);
    }

    private static LocalDate toDate(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof java.time.LocalDateTime t) {
            return t.toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }
}
