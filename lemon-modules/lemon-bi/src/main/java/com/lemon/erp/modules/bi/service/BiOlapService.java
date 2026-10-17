package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.bi.dto.PivotQuery;
import com.lemon.erp.modules.bi.dto.PivotRequest;
import com.lemon.erp.modules.bi.dto.PivotResult;
import com.lemon.erp.modules.bi.mapper.BiOlapMapper;
import com.lemon.erp.modules.bi.model.BiCube;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 多维分析：选择数据集、行维度、列维度、指标和筛选条件，在数据仓库事实表上汇总；支持钻取（前端把点击的维度值加入筛选并切换维度）。
 * 比率指标（毛利率、完工率、平均单价）按汇总后的分子 / 分母计算；行合计、列合计、总计分别查询，去重计数指标也准确
 */
@Service
@RequiredArgsConstructor
public class BiOlapService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BiOlapMapper olapMapper;

    /** 数据集、维度、指标的元数据 */
    public List<Map<String, Object>> cubes() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (BiCube cube : BiCube.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", cube.name());
            m.put("label", I18n.t(cube.getLabel()));
            m.put("dateFilter", cube.getDateFilter());
            m.put("dims", cube.getDims().stream()
                    .map(d -> new BiCube.BiDim(d.key(), I18n.t(d.label()), d.labelSource(), d.dictType())).toList());
            m.put("measures", cube.getMeasures().stream()
                    .map(x -> new BiCube.BiMeasure(x.key(), I18n.t(x.label()), x.format(), x.numerator(), x.denominator())).toList());
            list.add(m);
        }
        return list;
    }

    /** 数据权限规则：采购、库存不区分部门 */
    private static final Map<BiCube, DataPermissionRule> DATA_RULES = Map.of(
            BiCube.SALES, new DataPermissionRule("bi_fact_sales", "dept_id", "owner_id"),
            BiCube.PRODUCTION, new DataPermissionRule("bi_fact_production", "dept_id", null),
            BiCube.FINANCE, new DataPermissionRule("bi_fact_finance", "dept_id", null),
            BiCube.HR, new DataPermissionRule("bi_fact_hr", "dept_id", null),
            BiCube.CRM, new DataPermissionRule("bi_fact_crm", "dept_id", "owner_id"),
            BiCube.PROJECT, new DataPermissionRule("bi_fact_project", "dept_id", "manager_id"));

    public PivotResult pivot(PivotRequest req) {
        BiCube cube = cube(req.cube());
        String row = dim(cube, req.row());
        String col = dim(cube, req.col());
        if (row != null && row.equals(col)) {
            throw new BizException("行维度和列维度不能相同");
        }
        Map<String, List<String>> filters = new HashMap<>();
        if (req.filters() != null) {
            req.filters().forEach((k, v) -> {
                if (dim(cube, k) != null && v != null && !v.isEmpty()) {
                    filters.put(k, v.stream().map(String::valueOf).toList());
                }
            });
        }
        LocalDate[] range = range(req);
        boolean latest = cube == BiCube.INVENTORY && range[0] == null && range[1] == null
                && !"snapDate".equals(row) && !"snapDate".equals(col) && !filters.containsKey("snapDate");
        Function<PivotQuery, List<Map<String, Object>>> runner = runner(cube);
        PivotQuery base = new PivotQuery(row, col, range[0], range[1], toPeriod(range[0]), toPeriod(range[1]), latest, filters);
        List<Map<String, Object>> cells = finish(cube, runner.apply(base));
        List<Map<String, Object>> rowTotals = col == null ? cells : finish(cube, runner.apply(with(base, row, null)));
        List<Map<String, Object>> colTotals = col == null ? List.of() : finish(cube, runner.apply(with(base, null, col)));
        List<Map<String, Object>> totals = finish(cube, runner.apply(with(base, null, null)));
        Map<String, Map<String, String>> labels = new HashMap<>();
        labels.put("rows", labels(cube, row, rowTotals, "r"));
        labels.put("cols", labels(cube, col, colTotals, "c"));
        return new PivotResult(cells, rowTotals, colTotals, totals.isEmpty() ? Map.of() : totals.get(0), labels,
                range[0] == null ? null : range[0].toString(), range[1] == null ? null : range[1].toString());
    }

    /** 校验数据集、维度、日期范围（保存报表时） */
    public void validate(PivotRequest req) {
        BiCube cube = cube(req.cube());
        dim(cube, req.row());
        dim(cube, req.col());
        if (req.filters() != null) {
            req.filters().keySet().forEach(k -> dim(cube, k));
        }
        range(req);
    }

    // ---------------------------------------------------------------- 内部

    static BiCube cube(String key) {
        try {
            return BiCube.valueOf(key);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException("数据集不正确：{0}", key);
        }
    }

    private static String dim(BiCube cube, String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        if (cube.dim(key) == null) {
            throw new BizException("{0}没有维度：{1}", I18n.t(cube.getLabel()), key);
        }
        return key;
    }

    private Function<PivotQuery, List<Map<String, Object>>> runner(BiCube cube) {
        Function<PivotQuery, List<Map<String, Object>>> f = switch (cube) {
            case SALES -> olapMapper::pivotSales;
            case PURCHASE -> olapMapper::pivotPurchase;
            case INVENTORY -> olapMapper::pivotInventory;
            case PRODUCTION -> olapMapper::pivotProduction;
            case FINANCE -> olapMapper::pivotFinance;
            case HR -> olapMapper::pivotHr;
            case CRM -> olapMapper::pivotCrm;
            case PROJECT -> olapMapper::pivotProject;
        };
        DataPermissionRule rule = DATA_RULES.get(cube);
        if (rule == null || SecurityUtils.getDataScope().all()) {
            return f;
        }
        // 没有全部数据权限时按数据权限过滤事实表：销售按制单部门或客户负责人，生产、财务按部门
        return q -> DataPermissionContext.execute(rule, () -> f.apply(q));
    }

    private static PivotQuery with(PivotQuery q, String row, String col) {
        return new PivotQuery(row, col, q.begin(), q.end(), q.beginPeriod(), q.endPeriod(), q.latest(), q.filters());
    }

    /** 相对日期范围优先，ALL 表示不限 */
    static LocalDate[] range(PivotRequest req) {
        LocalDate today = LocalDate.now();
        if (!StringUtils.hasText(req.range())) {
            return new LocalDate[]{req.begin(), req.end()};
        }
        return switch (req.range()) {
            case "THIS_MONTH" -> new LocalDate[]{today.withDayOfMonth(1), today};
            case "THIS_QUARTER" -> new LocalDate[]{YearMonth.of(today.getYear(), (today.getMonthValue() - 1) / 3 * 3 + 1).atDay(1), today};
            case "THIS_YEAR" -> new LocalDate[]{today.withDayOfYear(1), today};
            case "LAST_12M" -> new LocalDate[]{YearMonth.from(today).minusMonths(11).atDay(1), today};
            case "ALL" -> new LocalDate[]{null, null};
            default -> throw new BizException("日期范围不正确：{0}", req.range());
        };
    }

    private static Integer toPeriod(LocalDate d) {
        return d == null ? null : d.getYear() * 100 + d.getMonthValue();
    }

    /** 维度值转为字符串（日期、数字统一），计算比率指标 */
    private static List<Map<String, Object>> finish(BiCube cube, List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            m.put("r", key(r.get("r")));
            m.put("c", key(r.get("c")));
            for (BiCube.BiMeasure measure : cube.getMeasures()) {
                if (measure.isRatio()) {
                    BigDecimal num = dec(m.get(measure.numerator()));
                    BigDecimal den = dec(m.get(measure.denominator()));
                    BigDecimal v = den.signum() == 0 ? null : "PRICE".equals(measure.format())
                            ? num.divide(den, 4, RoundingMode.HALF_UP)
                            : num.multiply(HUNDRED).divide(den, 2, RoundingMode.HALF_UP);
                    m.put(measure.key(), v);
                } else {
                    m.put(measure.key(), dec(m.get(measure.key())));
                }
            }
            result.add(m);
        }
        return result;
    }

    /**
     * ID 类维度的名称
     *
     * @param field 取值字段：r 行维度 / c 列维度
     */
    private Map<String, String> labels(BiCube cube, String dim, List<Map<String, Object>> rows, String field) {
        String source = dim == null ? null : cube.dim(dim).labelSource();
        Set<Object> ids = new HashSet<>();
        if (source != null) {
            for (Map<String, Object> r : rows) {
                Object v = r.get(field);
                if (v != null && !"-".equals(v)) {
                    ids.add(v);
                }
            }
        }
        Map<String, String> labels = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Map<String, Object> row : olapMapper.selectLabels(source, ids)) {
                labels.put(String.valueOf(row.get("k")), (String) row.get("v"));
            }
        }
        return labels;
    }

    private static String key(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal d) {
            return d.stripTrailingZeros().toPlainString();
        }
        return v.toString();
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
