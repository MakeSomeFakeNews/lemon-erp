package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.util.Decimals;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.entity.FinCommission;
import com.lemon.erp.modules.fin.entity.FinCommissionLine;
import com.lemon.erp.modules.fin.entity.FinCommissionRule;
import com.lemon.erp.modules.fin.mapper.FinCommissionLineMapper;
import com.lemon.erp.modules.fin.mapper.FinCommissionMapper;
import com.lemon.erp.modules.fin.mapper.FinCommissionQueryMapper;
import com.lemon.erp.modules.fin.mapper.FinCommissionRuleMapper;
import com.lemon.erp.modules.fin.model.CommissionBasis;
import com.lemon.erp.modules.fin.model.CommissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.fin.model.FinConstants.firstDay;
import static com.lemon.erp.modules.fin.model.FinConstants.lastDay;
import static com.lemon.erp.modules.fin.model.FinConstants.toYearMonth;

/**
 * 销售提成：按销售订单的业务员统计每月业绩（销售额、毛利、回款），按提成规则计算提成，确认后计入当月工资表（工资表生成时读取）。
 * <p>
 * 规则匹配：对每个业务员、每个依据、每个物料分类（回款不分分类），在已启用、依据相同、业务员为空或匹配、分类为空或匹配、
 * 门槛不超过该业务员当月该依据业绩合计的规则中，取最具体的（指定业务员优先于指定分类优先于通用），同样具体时取门槛最高的；
 * 提成 = 该分类的业绩 × 比例。不同依据的规则各自计算后相加（如同时按销售额和回款提成）
 */
@Service
@RequiredArgsConstructor
public class FinCommissionService {


    private final FinCommissionRuleMapper ruleMapper;
    private final FinCommissionMapper commissionMapper;
    private final FinCommissionLineMapper lineMapper;
    private final FinCommissionQueryMapper queryMapper;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------- 规则

    public List<FinCommissionRule> rules() {
        return queryMapper.selectRules();
    }

    @Transactional
    public Long saveRule(FinCommissionRule rule) {
        if (rule.getBasis() == CommissionBasis.RECEIPT && StringUtils.hasText(rule.getMaterialCategory())) {
            throw new BizException("fin.error.collectionBasedCommissionDoesNotDistinguish");
        }
        rule.setName(rule.getName().trim());
        rule.setMaterialCategory(StringUtils.hasText(rule.getMaterialCategory()) ? rule.getMaterialCategory() : null);
        rule.setThreshold(Decimals.nz(rule.getThreshold()));
        rule.setStatus(rule.getStatus() == null ? 1 : rule.getStatus());
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            if (ruleMapper.selectById(rule.getId()) == null) {
                throw new BizException("fin.error.commissionRuleNotFound");
            }
            ruleMapper.updateById(rule);
            // 可为空的字段
            ruleMapper.update(Wrappers.<FinCommissionRule>lambdaUpdate()
                    .set(FinCommissionRule::getSalesmanId, rule.getSalesmanId())
                    .set(FinCommissionRule::getMaterialCategory, rule.getMaterialCategory())
                    .set(FinCommissionRule::getRemark, rule.getRemark())
                    .eq(FinCommissionRule::getId, rule.getId()));
        }
        return rule.getId();
    }

    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 提成表

    /** 期间的提成表（含明细），没有时为空 */
    public FinCommission sheet(int period) {
        toYearMonth(period);
        FinCommission c = commissionMapper.selectOne(Wrappers.<FinCommission>lambdaQuery().eq(FinCommission::getPeriod, period));
        if (c != null) {
            c.setLines(queryMapper.selectLines(c.getId()));
        }
        return c;
    }

    /** 已生成的提成表期间，倒序 */
    public List<FinCommission> sheets() {
        return commissionMapper.selectList(Wrappers.<FinCommission>lambdaQuery().orderByDesc(FinCommission::getPeriod));
    }

    /**
     * 计算（或重新计算）期间的提成表：草稿时覆盖，保留各业务员的手工调整
     *
     * @return 提成表 ID
     */
    @Transactional
    public Long generate(int period) {
        toYearMonth(period);
        FinCommission sheet = commissionMapper.selectOne(Wrappers.<FinCommission>lambdaQuery().eq(FinCommission::getPeriod, period));
        if (sheet != null && sheet.getStatus() != CommissionStatus.DRAFT) {
            throw new BizException("fin.error.commissionSheetConfirmedUnconfirmFirst", period);
        }
        Map<Long, FinCommissionLine> oldLines = new HashMap<>();
        if (sheet == null) {
            sheet = new FinCommission();
            sheet.setPeriod(period);
            sheet.setStatus(CommissionStatus.DRAFT);
            sheet.setTotalAmount(BigDecimal.ZERO);
            commissionMapper.insert(sheet);
        } else {
            lineMapper.selectList(Wrappers.<FinCommissionLine>lambdaQuery().eq(FinCommissionLine::getCommissionId, sheet.getId()))
                    .forEach(l -> oldLines.put(l.getSalesmanId(), l));
            lineMapper.delete(Wrappers.<FinCommissionLine>lambdaQuery().eq(FinCommissionLine::getCommissionId, sheet.getId()));
        }
        List<Perf> perfs = performance(period);
        List<FinCommissionRule> rules = ruleMapper.selectList(Wrappers.<FinCommissionRule>lambdaQuery().eq(FinCommissionRule::getStatus, 1));
        BigDecimal total = BigDecimal.ZERO;
        for (Perf perf : perfs) {
            List<Map<String, Object>> detail = new ArrayList<>();
            BigDecimal commission = BigDecimal.ZERO;
            Set<CommissionBasis> bases = rules.stream().map(FinCommissionRule::getBasis).collect(Collectors.toCollection(TreeSet::new));
            for (CommissionBasis basis : bases) {
                BigDecimal achievement = perf.total(basis);
                Map<String, BigDecimal> buckets = basis == CommissionBasis.RECEIPT ? Map.of("", perf.receipt) : perf.byCategory(basis);
                for (Map.Entry<String, BigDecimal> b : buckets.entrySet()) {
                    String category = b.getKey().isEmpty() ? null : b.getKey();
                    FinCommissionRule rule = match(rules, basis, perf.salesmanId, category, achievement);
                    if (rule == null || b.getValue().signum() == 0) {
                        continue;
                    }
                    BigDecimal amount = b.getValue().multiply(rule.getRate()).divide(Decimals.HUNDRED, 2, RoundingMode.HALF_UP);
                    commission = commission.add(amount);
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("basis", basis.name());
                    d.put("category", category);
                    d.put("base", b.getValue());
                    d.put("ruleId", rule.getId());
                    d.put("ruleName", rule.getName());
                    d.put("rate", rule.getRate());
                    d.put("amount", amount);
                    detail.add(d);
                }
            }
            FinCommissionLine old = oldLines.get(perf.salesmanId);
            FinCommissionLine line = new FinCommissionLine();
            line.setCommissionId(sheet.getId());
            line.setSalesmanId(perf.salesmanId);
            line.setSalesAmount(perf.sales());
            line.setProfitAmount(perf.profit());
            line.setReceiptAmount(perf.receipt);
            line.setCommissionAmount(commission);
            line.setAdjustAmount(old == null ? BigDecimal.ZERO : old.getAdjustAmount());
            line.setFinalAmount(commission.add(line.getAdjustAmount()));
            line.setRemark(old == null ? null : old.getRemark());
            line.setDetail(objectMapper.writeValueAsString(detail));
            lineMapper.insert(line);
            total = total.add(line.getFinalAmount());
        }
        commissionMapper.update(Wrappers.<FinCommission>lambdaUpdate().set(FinCommission::getTotalAmount, total)
                .set(FinCommission::getUpdateTime, LocalDateTime.now()).eq(FinCommission::getId, sheet.getId()));
        return sheet.getId();
    }

    /** 手工调整某个业务员的提成（草稿） */
    @Transactional
    public void adjust(Long lineId, BigDecimal adjust, String remark) {
        FinCommissionLine line = lineMapper.selectById(lineId);
        if (line == null) {
            throw new BizException("fin.error.commissionDetailNotFound");
        }
        FinCommission sheet = commissionMapper.selectById(line.getCommissionId());
        if (sheet.getStatus() != CommissionStatus.DRAFT) {
            throw new BizException("fin.error.commissionSheetConfirmedCannotAdjusted");
        }
        BigDecimal a = Decimals.nz(adjust);
        lineMapper.update(Wrappers.<FinCommissionLine>lambdaUpdate()
                .set(FinCommissionLine::getAdjustAmount, a)
                .set(FinCommissionLine::getFinalAmount, line.getCommissionAmount().add(a))
                .set(FinCommissionLine::getRemark, remark)
                .eq(FinCommissionLine::getId, lineId));
        refreshTotal(sheet.getId());
    }

    /** 确认：之后生成的工资表计入销售提成 */
    @Transactional
    public void confirm(int period) {
        FinCommission sheet = requireSheet(period);
        StatusTransitions.change(commissionMapper, FinCommission::getId, sheet.getId(), FinCommission::getStatus,
                CommissionStatus.DRAFT, CommissionStatus.CONFIRMED, w -> w
                        .set(FinCommission::getConfirmBy, SecurityUtils.getUserId())
                        .set(FinCommission::getConfirmTime, LocalDateTime.now()), "fin.error.commissionSheetAlreadyConfirmed");
    }

    /** 取消确认（已生成的工资表不会自动更新，需重新生成草稿工资表） */
    @Transactional
    public void unconfirm(int period) {
        FinCommission sheet = requireSheet(period);
        commissionMapper.update(Wrappers.<FinCommission>lambdaUpdate()
                .set(FinCommission::getStatus, CommissionStatus.DRAFT)
                .set(FinCommission::getConfirmBy, null)
                .set(FinCommission::getConfirmTime, null)
                .eq(FinCommission::getId, sheet.getId()));
    }

    /** 已确认的提成：业务员（用户）→ 应发提成，工资表生成时读取；没有已确认的提成表时为空 */
    public Map<Long, BigDecimal> confirmedAmounts(int period) {
        FinCommission sheet = commissionMapper.selectOne(Wrappers.<FinCommission>lambdaQuery()
                .eq(FinCommission::getPeriod, period).eq(FinCommission::getStatus, CommissionStatus.CONFIRMED));
        if (sheet == null) {
            return Map.of();
        }
        return lineMapper.selectList(Wrappers.<FinCommissionLine>lambdaQuery().eq(FinCommissionLine::getCommissionId, sheet.getId()))
                .stream().collect(Collectors.toMap(FinCommissionLine::getSalesmanId, FinCommissionLine::getFinalAmount, BigDecimal::add));
    }

    // ---------------------------------------------------------------- 内部

    /** 最具体（指定业务员 2 分 + 指定分类 1 分）、门槛最高的已达成规则 */
    static FinCommissionRule match(List<FinCommissionRule> rules, CommissionBasis basis, Long salesmanId, String category, BigDecimal achievement) {
        return rules.stream()
                .filter(r -> r.getBasis() == basis)
                .filter(r -> r.getSalesmanId() == null || r.getSalesmanId().equals(salesmanId))
                .filter(r -> r.getMaterialCategory() == null || Objects.equals(r.getMaterialCategory(), category))
                .filter(r -> r.getThreshold().compareTo(achievement) <= 0)
                .max(Comparator.comparingInt((FinCommissionRule r) -> (r.getSalesmanId() != null ? 2 : 0) + (r.getMaterialCategory() != null ? 1 : 0))
                        .thenComparing(FinCommissionRule::getThreshold))
                .orElse(null);
    }

    private List<Perf> performance(int period) {
        LocalDate begin = firstDay(period);
        LocalDate end = lastDay(period);
        List<Map<String, Object>> sales = queryMapper.selectSales(begin, end);
        Set<Long> materialIds = sales.stream().map(r -> ((Number) r.get("materialId")).longValue()).collect(Collectors.toSet());
        Map<Long, BigDecimal> units = new HashMap<>();
        if (!materialIds.isEmpty()) {
            queryMapper.selectUnitCosts(period, materialIds).forEach(r -> units.put(((Number) r.get("materialId")).longValue(), dec(r.get("unitCost"))));
        }
        Map<Long, Perf> perfs = new LinkedHashMap<>();
        for (Map<String, Object> r : sales) {
            Long salesman = ((Number) r.get("salesmanId")).longValue();
            String category = r.get("category") == null ? "" : (String) r.get("category");
            BigDecimal net = dec(r.get("net"));
            BigDecimal cost = dec(r.get("qty")).multiply(units.getOrDefault(((Number) r.get("materialId")).longValue(), BigDecimal.ZERO));
            Perf p = perfs.computeIfAbsent(salesman, Perf::new);
            p.salesByCategory.merge(category, net, BigDecimal::add);
            p.profitByCategory.merge(category, net.subtract(cost).setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        }
        for (Map<String, Object> r : queryMapper.selectReceipts(begin, end)) {
            Long salesman = ((Number) r.get("salesmanId")).longValue();
            perfs.computeIfAbsent(salesman, Perf::new).receipt = dec(r.get("amount"));
        }
        return new ArrayList<>(perfs.values());
    }

    private FinCommission requireSheet(int period) {
        FinCommission sheet = commissionMapper.selectOne(Wrappers.<FinCommission>lambdaQuery().eq(FinCommission::getPeriod, period));
        if (sheet == null) {
            throw new BizException("fin.error.commissionHasNotCalculated", period);
        }
        return sheet;
    }

    private void refreshTotal(Long sheetId) {
        BigDecimal total = Decimals.sum(lineMapper.selectList(Wrappers.<FinCommissionLine>lambdaQuery()
                .eq(FinCommissionLine::getCommissionId, sheetId)), FinCommissionLine::getFinalAmount);
        commissionMapper.update(Wrappers.<FinCommission>lambdaUpdate().set(FinCommission::getTotalAmount, total).eq(FinCommission::getId, sheetId));
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    /** 一个业务员的当月业绩：销售额、毛利按物料分类，回款合计 */
    private static final class Perf {
        final Long salesmanId;
        final Map<String, BigDecimal> salesByCategory = new LinkedHashMap<>();
        final Map<String, BigDecimal> profitByCategory = new LinkedHashMap<>();
        BigDecimal receipt = BigDecimal.ZERO;

        Perf(Long salesmanId) {
            this.salesmanId = salesmanId;
        }

        BigDecimal sales() {
            return Decimals.sum(salesByCategory.values()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal profit() {
            return Decimals.sum(profitByCategory.values()).setScale(2, RoundingMode.HALF_UP);
        }

        Map<String, BigDecimal> byCategory(CommissionBasis basis) {
            return basis == CommissionBasis.SALES ? salesByCategory : profitByCategory;
        }

        BigDecimal total(CommissionBasis basis) {
            return switch (basis) {
                case SALES -> sales();
                case PROFIT -> profit();
                case RECEIPT -> receipt;
            };
        }
    }
}
