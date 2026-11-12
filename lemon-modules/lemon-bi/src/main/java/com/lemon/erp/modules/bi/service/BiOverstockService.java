package com.lemon.erp.modules.bi.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.bi.dto.JxcViews;
import com.lemon.erp.modules.bi.dto.OverstockQuery;
import com.lemon.erp.modules.bi.mapper.BiJxcMapper;
import com.lemon.erp.modules.bi.model.OverstockBase;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 积压分析（进销存报表，issue #111）：截至某日库存数量超过积压基数的商品。
 * 积压基数为库存上限（物料的上下限设置）、上个月销量或最近 N 天销量（销售出库 − 销售退货，按所选仓库，口径同销售报表）；
 * 积压数量 = 库存数量 − 积压基数，只列出大于 0 的。销量按出入库单的制单人部门 + 制单人做数据权限（与商品分析一致）
 */
@Service
@RequiredArgsConstructor
public class BiOverstockService {

    /** 最近 N 天的默认天数 */
    static final int DEFAULT_DAYS = 60;
    /** 最近 N 天的上限 */
    static final int MAX_DAYS = 366;
    /** 积压系数的小数位 */
    private static final int RATIO_SCALE = 2;

    private final BiJxcMapper jxcMapper;
    private final PermissionService permissionService;

    @DataPermission(table = "scm_trade_doc", deptColumn = "dept_id", userColumn = "create_by")
    public JxcViews.OverstockPage page(OverstockQuery q, long current, long size) {
        prepare(q);
        Page<JxcViews.OverstockRow> page = jxcMapper.selectOverstock(Page.of(current, size), q);
        for (JxcViews.OverstockRow r : page.getRecords()) {
            fill(r, q);
        }
        JxcViews.OverstockRow summary = jxcMapper.selectOverstockTotal(q);
        if (summary == null) {
            summary = new JxcViews.OverstockRow();
        }
        summary.setAmount(q.isCostVisible() ? Decimals.money(summary.getAmount()) : null);
        boolean bySales = q.getBase() != OverstockBase.MAX_STOCK;
        return new JxcViews.OverstockPage(page.getRecords(), page.getTotal(), summary, q.getDate(),
                bySales ? q.getSalesBegin() : null, bySales ? q.getSalesEnd() : null, q.isCostVisible());
    }

    /** 默认值与校验：截至日期不晚于今天，默认今天；积压基数默认最近 N 天销量，N 为 1–366，默认 60 */
    void prepare(OverstockQuery q) {
        LocalDate today = LocalDate.now();
        if (q.getDate() == null) {
            q.setDate(today);
        } else if (q.getDate().isAfter(today)) {
            throw new BizException("bi.error.overstockDateAfterToday");
        }
        if (q.getBase() == null) {
            q.setBase(OverstockBase.RECENT_DAYS);
        }
        if (q.getDays() == null) {
            q.setDays(DEFAULT_DAYS);
        } else if (q.getDays() < 1 || q.getDays() > MAX_DAYS) {
            throw new BizException("bi.error.overstockDaysRange", MAX_DAYS);
        }
        LocalDate[] range = salesRange(q.getBase(), q.getDate(), q.getDays());
        q.resolve(range[0], range[1], permissionService.hasPerm(WmsStockDocService.COST_PERM));
    }

    /**
     * 销量的统计期间：上个月为截至日所在月份的上一个自然月；最近 N 天为截至日（含）往前 N 天；库存上限不需要（返回截至日）
     *
     * @return [开始, 结束]
     */
    static LocalDate[] salesRange(OverstockBase base, LocalDate date, int days) {
        if (base == OverstockBase.LAST_MONTH) {
            LocalDate first = date.withDayOfMonth(1).minusMonths(1);
            return new LocalDate[]{first, first.plusMonths(1).minusDays(1)};
        }
        if (base == OverstockBase.RECENT_DAYS) {
            return new LocalDate[]{date.minusDays(days - 1L), date};
        }
        return new LocalDate[]{date, date};
    }

    /** 在库天数、积压系数；没有成本权限时不返回资金占用 */
    private static void fill(JxcViews.OverstockRow r, OverstockQuery q) {
        LocalDate since = r.getLastPurchaseDate() != null ? r.getLastPurchaseDate() : r.getCreateDate();
        r.setStockDays(since == null ? null : Math.max(0, ChronoUnit.DAYS.between(since, q.getDate())));
        r.setRatio(ratio(r.getStockQty(), r.getBaseQty()));
        r.setAmount(q.isCostVisible() ? Decimals.money(r.getAmount()) : null);
    }

    /** 积压系数 = 库存数量 ÷ 积压基数，基数不大于 0 时为空 */
    static BigDecimal ratio(BigDecimal stock, BigDecimal base) {
        if (base == null || base.signum() <= 0) {
            return null;
        }
        return Decimals.nz(stock).divide(base, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
