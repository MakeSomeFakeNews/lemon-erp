package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.mes.dto.MesStatQuery;
import com.lemon.erp.modules.mes.dto.MesStatViews;
import com.lemon.erp.modules.mes.mapper.MesStatMapper;
import com.lemon.erp.modules.mes.model.MesStatDimension;
import com.lemon.erp.modules.mes.model.WoDueState;
import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.lemon.erp.modules.mes.model.MesStatDimension.CATEGORY;
import static com.lemon.erp.modules.mes.model.MesStatDimension.DAY;
import static com.lemon.erp.modules.mes.model.MesStatDimension.DEPT;
import static com.lemon.erp.modules.mes.model.MesStatDimension.MATERIAL;
import static com.lemon.erp.modules.mes.model.MesStatDimension.MONTH;
import static com.lemon.erp.modules.mes.model.MesStatDimension.OPERATION;
import static com.lemon.erp.modules.mes.model.MesStatDimension.OPERATOR;
import static com.lemon.erp.modules.mes.model.MesStatDimension.OPERATOR_DAY;
import static com.lemon.erp.modules.mes.model.MesStatDimension.OPERATOR_OP;
import static com.lemon.erp.modules.mes.model.MesStatDimension.PRODUCT;
import static com.lemon.erp.modules.mes.model.MesStatDimension.WORK_CENTER;
import static com.lemon.erp.modules.mes.model.MesStatDimension.WORK_ORDER;
import static com.lemon.erp.modules.mes.model.MesStatDimension.WO_MATERIAL;

/**
 * 生产统计报表（「生产制造 → 生产统计」），实时汇总生产单据，按工单的部门 + 制单人做数据权限：
 * <ul>
 *   <li>人员计件统计：报工按操作员（可再按工序、日期）汇总合格、报废、工时、计件工资（Σ 报工的计件工资 = 合格数 × 工序计件单价，
 *       与工资表的计件工资同一口径），可穿透到报工明细</li>
 *   <li>工序 / 工作中心产量统计：报工按工作中心、工序、产品、日、月汇总产量、报废、良品率、工时、标准工时和效率</li>
 *   <li>工单完成情况：计划完工日期在期间内的工单（不含草稿）的计划、完工、报废、完成率、良品率和交期（按期 / 延期完工、逾期未完工）</li>
 *   <li>产品产量汇总：完工入库、报废入库数量和完工入库成本（按产品、分类、车间、日、月）</li>
 *   <li>用料差异：期间内完工或关闭的工单，标准用量（完工数量 × 单位用量，含损耗）与净领料的差异</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MesStatService {

    /** 最大日期跨度（天） */
    static final int MAX_DAYS = 366;
    /** 汇总最多返回的行数 */
    static final int LIMIT = 2000;

    private static final Set<MesStatDimension> PIECE_DIMS = EnumSet.of(OPERATOR, OPERATOR_OP, OPERATOR_DAY);
    private static final Set<MesStatDimension> OUTPUT_DIMS = EnumSet.of(WORK_CENTER, OPERATION, PRODUCT, DAY, MONTH);
    private static final Set<MesStatDimension> WO_DIMS = EnumSet.of(WORK_ORDER, PRODUCT, DEPT, MONTH);
    private static final Set<MesStatDimension> PRODUCT_DIMS = EnumSet.of(PRODUCT, CATEGORY, DEPT, DAY, MONTH);
    private static final Set<MesStatDimension> USAGE_DIMS = EnumSet.of(MATERIAL, WO_MATERIAL);

    private final MesStatMapper statMapper;
    private final PermissionService permissionService;

    // ---------------------------------------------------------------- 报工：人员计件、工序 / 工作中心产量

    /** 人员计件统计 */
    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public MesStatViews.Summary<MesStatViews.ReportRow> piece(MesStatDimension dim, MesStatQuery q) {
        return reportSummary(PIECE_DIMS, dim, q);
    }

    /** 工序 / 工作中心产量统计 */
    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public MesStatViews.Summary<MesStatViews.ReportRow> output(MesStatDimension dim, MesStatQuery q) {
        return reportSummary(OUTPUT_DIMS, dim, q);
    }

    /** 报工明细（汇总行穿透） */
    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<MesStatViews.ReportDetail> reportDetail(MesStatQuery q, long current, long size) {
        prepare(q);
        return PageResult.of(statMapper.selectReportDetail(Page.of(current, size), q));
    }

    private MesStatViews.Summary<MesStatViews.ReportRow> reportSummary(Set<MesStatDimension> allowed, MesStatDimension dim, MesStatQuery q) {
        prepare(q);
        checkDim(allowed, dim);
        List<MesStatViews.ReportRow> list = statMapper.selectReportSummary(dim, q, LIMIT + 1);
        List<MesStatViews.ReportRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), LIMIT)));
        rows.forEach(r -> {
            r.setGroupKey(key(r.getOperatorId(), r.getWorkCenterId(), r.getOpName(), r.getProductId(), r.getDay(), r.getPeriod()));
            fillReport(r);
        });
        MesStatViews.ReportRow total = statMapper.selectReportTotal(q);
        if (total == null) {
            total = new MesStatViews.ReportRow();
        }
        fillReport(total);
        return new MesStatViews.Summary<>(rows, total, list.size() > LIMIT);
    }

    private static void fillReport(MesStatViews.ReportRow r) {
        r.setGoodQty(Decimals.qty(r.getGoodQty()));
        r.setScrapQty(Decimals.qty(r.getScrapQty()));
        r.setHours(Decimals.qty(r.getHours()));
        r.setStdHours(Decimals.qty(r.getStdHours()));
        r.setPieceWage(Decimals.money(r.getPieceWage()));
        r.setYieldRate(Decimals.percent(r.getGoodQty(), r.getGoodQty().add(r.getScrapQty()), 1));
        r.setEfficiency(r.getStdHours().signum() > 0 ? Decimals.percent(r.getStdHours(), r.getHours(), 1) : null);
        r.setReportCount(nz(r.getReportCount()));
        r.setWoCount(nz(r.getWoCount()));
        r.setOperatorCount(nz(r.getOperatorCount()));
    }

    // ---------------------------------------------------------------- 工单完成情况

    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public MesStatViews.Summary<MesStatViews.WoRow> workOrders(MesStatDimension dim, MesStatQuery q) {
        prepare(q);
        checkDim(WO_DIMS, dim);
        LocalDate today = LocalDate.now();
        List<MesStatViews.WoRow> list = dim == WORK_ORDER ? statMapper.selectWoList(q, LIMIT + 1)
                : statMapper.selectWoSummary(dim, q, today, LIMIT + 1);
        List<MesStatViews.WoRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), LIMIT)));
        for (MesStatViews.WoRow r : rows) {
            if (dim == WORK_ORDER) {
                r.setGroupKey(String.valueOf(r.getWoId()));
                due(r, today);
            } else {
                r.setGroupKey(key(r.getProductId(), r.getDeptId(), r.getPeriod()));
            }
            fillWo(r);
        }
        List<MesStatViews.WoRow> totals = statMapper.selectWoSummary(null, q, today, 1);
        MesStatViews.WoRow total = totals.isEmpty() || totals.get(0) == null ? new MesStatViews.WoRow() : totals.get(0);
        fillWo(total);
        return new MesStatViews.Summary<>(rows, total, list.size() > LIMIT);
    }

    /** 单个工单的交期状态和延期天数（与汇总的口径一致：完工日期晚于计划完工日期为延期） */
    static void due(MesStatViews.WoRow r, LocalDate today) {
        boolean done = r.getStatus() == WorkOrderStatus.COMPLETED;
        r.setWoCount(1L);
        r.setCompletedCount(done ? 1L : 0L);
        r.setClosedCount(r.getStatus() == WorkOrderStatus.CLOSED ? 1L : 0L);
        LocalDate planEnd = r.getPlanEnd();
        if (r.getStatus() == WorkOrderStatus.CLOSED) {
            r.setDueState(WoDueState.CLOSED);
        } else if (done) {
            LocalDate finish = r.getFinishTime() == null ? planEnd : r.getFinishTime().toLocalDate();
            boolean late = planEnd != null && finish != null && finish.isAfter(planEnd);
            r.setDueState(late ? WoDueState.LATE : WoDueState.ON_TIME);
            r.setDelayDays(late ? (int) ChronoUnit.DAYS.between(planEnd, finish) : null);
        } else if (planEnd != null && planEnd.isBefore(today)) {
            r.setDueState(WoDueState.OVERDUE);
            r.setDelayDays((int) ChronoUnit.DAYS.between(planEnd, today));
        } else {
            r.setDueState(WoDueState.OPEN);
        }
        r.setOnTimeCount(r.getDueState() == WoDueState.ON_TIME ? 1L : 0L);
        r.setLateCount(r.getDueState() == WoDueState.LATE ? 1L : 0L);
        r.setOverdueCount(r.getDueState() == WoDueState.OVERDUE ? 1L : 0L);
    }

    private static void fillWo(MesStatViews.WoRow r) {
        r.setQty(Decimals.qty(r.getQty()));
        r.setFinishedQty(Decimals.qty(r.getFinishedQty()));
        r.setScrapQty(Decimals.qty(r.getScrapQty()));
        r.setCompletionRate(Decimals.percent(r.getFinishedQty(), r.getQty(), 1));
        r.setYieldRate(Decimals.percent(r.getFinishedQty(), r.getFinishedQty().add(r.getScrapQty()), 1));
        r.setWoCount(nz(r.getWoCount()));
        r.setCompletedCount(nz(r.getCompletedCount()));
        r.setOnTimeCount(nz(r.getOnTimeCount()));
        r.setLateCount(nz(r.getLateCount()));
        r.setOverdueCount(nz(r.getOverdueCount()));
        r.setClosedCount(nz(r.getClosedCount()));
        r.setOnTimeRate(Decimals.percent(BigDecimal.valueOf(r.getOnTimeCount()), BigDecimal.valueOf(r.getCompletedCount()), 1));
    }

    // ---------------------------------------------------------------- 产品产量

    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public MesStatViews.Summary<MesStatViews.OutputRow> products(MesStatDimension dim, MesStatQuery q) {
        prepare(q);
        checkDim(PRODUCT_DIMS, dim);
        boolean costVisible = permissionService.hasPerm(WmsStockDocService.COST_PERM);
        List<MesStatViews.OutputRow> list = statMapper.selectOutputSummary(dim, q, LIMIT + 1);
        List<MesStatViews.OutputRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), LIMIT)));
        rows.forEach(r -> {
            r.setGroupKey(key(r.getProductId(), r.getCategory(), r.getDeptId(), r.getDay(), r.getPeriod()));
            fillOutput(r, costVisible);
        });
        List<MesStatViews.OutputRow> totals = statMapper.selectOutputSummary(null, q, 1);
        MesStatViews.OutputRow total = totals.isEmpty() || totals.get(0) == null ? new MesStatViews.OutputRow() : totals.get(0);
        fillOutput(total, costVisible);
        return new MesStatViews.Summary<>(rows, total, list.size() > LIMIT);
    }

    private static void fillOutput(MesStatViews.OutputRow r, boolean costVisible) {
        r.setFinishQty(Decimals.qty(r.getFinishQty()));
        r.setScrapInQty(Decimals.qty(r.getScrapInQty()));
        r.setFinishCount(nz(r.getFinishCount()));
        r.setWoCount(nz(r.getWoCount()));
        if (costVisible) {
            r.setFinishCost(Decimals.money(r.getFinishCost()));
            r.setUnitCost(r.getFinishQty().signum() > 0 ? r.getFinishCost().divide(r.getFinishQty(), 4, RoundingMode.HALF_UP) : null);
        } else {
            r.setFinishCost(null);
            r.setUnitCost(null);
        }
    }

    // ---------------------------------------------------------------- 用料差异

    @DataPermission(table = "mes_work_order", deptColumn = "dept_id", userColumn = "create_by")
    public MesStatViews.Summary<MesStatViews.UsageRow> usage(MesStatDimension dim, MesStatQuery q) {
        prepare(q);
        checkDim(USAGE_DIMS, dim);
        List<MesStatViews.UsageRow> list = statMapper.selectUsage(dim, q, q.getBegin().atStartOfDay(), q.getEnd().plusDays(1).atStartOfDay(),
                LIMIT + 1);
        List<MesStatViews.UsageRow> rows = new ArrayList<>(list.subList(0, Math.min(list.size(), LIMIT)));
        rows.forEach(r -> {
            r.setGroupKey(key(r.getWoId(), r.getMaterialId()));
            if (r.getWoCount() == null) {
                r.setWoCount(1L);
            }
            r.setStdQty(Decimals.qty(r.getStdQty()));
            r.setActualQty(Decimals.qty(r.getActualQty()));
            r.setVarianceQty(r.getActualQty().subtract(r.getStdQty()));
            r.setVarianceRate(Decimals.percent(r.getVarianceQty(), r.getStdQty(), 1));
        });
        // 不同物料的单位不同，不提供数量合计
        return new MesStatViews.Summary<>(rows, null, list.size() > LIMIT);
    }

    // ---------------------------------------------------------------- 内部

    private static void prepare(MesStatQuery q) {
        if (q.getBegin() == null || q.getEnd() == null) {
            throw new BizException("mes.error.statDateRangeRequired");
        }
        if (q.getEnd().isBefore(q.getBegin())) {
            throw new BizException("mes.error.statEndBeforeBegin");
        }
        if (ChronoUnit.DAYS.between(q.getBegin(), q.getEnd()) >= MAX_DAYS) {
            throw new BizException("mes.error.statDateRangeTooLong", MAX_DAYS);
        }
        if (q.getKeyword() != null) {
            q.setKeyword(StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null);
        }
        if (q.getOpName() != null && !StringUtils.hasText(q.getOpName())) {
            q.setOpName(null);
        }
    }

    private static void checkDim(Set<MesStatDimension> allowed, MesStatDimension dim) {
        if (dim == null || !allowed.contains(dim)) {
            throw new BizException("mes.error.statInvalidDimension", String.valueOf(dim));
        }
    }

    /** 汇总行的键：各维度取值用 | 连接（空值为空串） */
    private static String key(Object... parts) {
        List<String> list = new ArrayList<>();
        for (Object p : parts) {
            list.add(p == null ? "" : String.valueOf(p));
        }
        return String.join("|", list);
    }

    private static Long nz(Long v) {
        return v == null ? 0L : v;
    }
}
