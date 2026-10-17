package com.lemon.erp.modules.fin.service.cost;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.fin.entity.FinMaterialCost;
import com.lemon.erp.modules.fin.entity.FinWoCost;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.model.AccountRole;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.service.FinAccountRuleService;
import com.lemon.erp.modules.fin.service.FinVoucherService;
import com.lemon.erp.modules.fin.service.cost.CostCalculator.CostResult;
import com.lemon.erp.modules.fin.service.cost.CostRows.Assembly;
import com.lemon.erp.modules.fin.service.cost.CostRows.ConsumeRow;
import com.lemon.erp.modules.fin.service.cost.CostRows.CostMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.lemon.erp.modules.fin.service.cost.Valuation.money;

/**
 * 成本结转凭证（未审核）：领料 借 生产成本 / 贷 原材料、库存商品；人工、制造费用 借 生产成本 / 贷 制造费用
 * （生产岗位工资计提时计入制造费用）；制造费用差异（期末制造费用余额）转主营业务成本；关闭工单的剩余在产品转主营业务成本；
 * 完工 借 库存商品 / 贷 生产成本；销售成本 借 主营业务成本 / 贷 存货；项目领料 借 合同履约成本；委外发料 借 委托加工物资；
 * 其他出入库 待处理财产损溢（填写了收发类别的按类别取凭证科目设置中的对方科目）
 */
@Component
@RequiredArgsConstructor
public class CostVoucherWriter {

    private final FinVoucherEntryMapper entryMapper;
    private final FinAccountRuleService accountRules;
    private final FinVoucherService voucherService;

    /**
     * 生成的凭证
     *
     * @param voucherId        凭证 ID，没有非零分录时为空
     * @param overheadVariance 制造费用差异（实际发生 - 按费率分配），转入主营业务成本
     */
    public record CostVoucher(Long voucherId, BigDecimal overheadVariance) {
    }

    /** 生成成本结转凭证（业务日期为期末），所有分录都为 0 时不生成 */
    public CostVoucher write(CostSource src, CostResult result, LocalDate end) {
        Lines lines = lines(src, result);
        Long voucherId = lines.lines().isEmpty() ? null
                : voucherService.createGenerated(end, VoucherSource.COST, String.valueOf(src.getPeriod()), lines.lines());
        return new CostVoucher(voucherId, lines.overheadVariance());
    }

    /** 凭证分录（全部为 0 时为空）与制造费用差异 */
    record Lines(List<VoucherLine> lines, BigDecimal overheadVariance) {
    }

    Lines lines(CostSource src, CostResult result) {
        int period = src.getPeriod();
        Map<Long, FinMaterialCost> materials = result.materials();
        Map<Long, FinWoCost> woCosts = result.woCosts();
        Function<Long, String> category = materialId -> {
            CostMaterial m = src.getMaterials().get(materialId);
            return m == null ? null : m.category();
        };
        String summary = "结转 " + period + " 期间成本"; // i18n-ignore
        Long wip = accountRules.accountId(AccountRole.PRODUCTION_COST);
        // 物料分类 → 存货科目
        Map<String, Long> inventoryAccounts = new HashMap<>();
        Function<String, Long> inventory = cat -> inventoryAccounts.computeIfAbsent(cat,
                k -> accountRules.accountId(AccountRole.INVENTORY, k));
        List<VoucherLine> lines = new ArrayList<>();
        // 领料：按领用物料的存货科目，金额取工单成本中的材料成本合计（与工单口径一致）
        Map<Long, BigDecimal> issueByAccount = new HashMap<>();
        materials.values().forEach(c -> issueByAccount.merge(inventory.apply(category.apply(c.getMaterialId())), c.getIssueAmount(), BigDecimal::add));
        BigDecimal issueTotal = Decimals.sum(issueByAccount.values());
        BigDecimal woMaterial = Decimals.sum(woCosts.values(), FinWoCost::getMaterialCost);
        lines.add(VoucherLine.debit(summary + "-生产领料", wip, null, woMaterial)); // i18n-ignore
        issueByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-生产领料", code, null, amount))); // i18n-ignore
        // 工单材料成本与物料领用金额的尾差（四舍五入）计入生产成本
        BigDecimal diff = issueTotal.subtract(woMaterial);
        if (diff.signum() != 0) {
            lines.add(VoucherLine.debit(summary + "-生产领料", wip, null, diff)); // i18n-ignore
        }
        // 人工、制造费用按工时 × 费率分配到工单：生产岗位工资计提时已计入制造费用，这里从制造费用转出
        Long overheadAccount = accountRules.accountId(AccountRole.MANUFACTURING_OVERHEAD);
        BigDecimal labor = Decimals.sum(woCosts.values(), FinWoCost::getLaborCost);
        lines.add(VoucherLine.debit(summary + "-直接人工", wip, null, labor)); // i18n-ignore
        lines.add(VoucherLine.credit(summary + "-直接人工", overheadAccount, null, labor)); // i18n-ignore
        BigDecimal overhead = Decimals.sum(woCosts.values(), FinWoCost::getOverheadCost);
        lines.add(VoucherLine.debit(summary + "-制造费用", wip, null, overhead)); // i18n-ignore
        lines.add(VoucherLine.credit(summary + "-制造费用", overheadAccount, null, overhead)); // i18n-ignore
        // 制造费用差异：截至期末的制造费用余额（实际发生 - 已分配）全部转入主营业务成本
        BigDecimal balance = entryMapper.sumBalanceUpTo(overheadAccount, period);
        BigDecimal overheadVariance = money(Decimals.nz(balance).subtract(labor).subtract(overhead));
        lines.add(VoucherLine.debit(summary + "-制造费用差异", accountRules.accountId(AccountRole.COST_OF_SALES), null, overheadVariance)); // i18n-ignore
        lines.add(VoucherLine.credit(summary + "-制造费用差异", overheadAccount, null, overheadVariance)); // i18n-ignore
        // 关闭工单的剩余在产品转生产损失（计入主营业务成本）
        BigDecimal loss = Decimals.sum(woCosts.values(), FinWoCost::getLossAmount);
        lines.add(VoucherLine.debit(summary + "-关闭工单损失", accountRules.accountId(AccountRole.COST_OF_SALES), null, loss)); // i18n-ignore
        lines.add(VoucherLine.credit(summary + "-关闭工单损失", wip, null, loss)); // i18n-ignore
        // 完工入库：按产品的存货科目
        Map<Long, BigDecimal> finishedByAccount = new HashMap<>();
        for (FinWoCost w : woCosts.values()) {
            Long product = src.getWorkOrders().get(w.getWoId()).materialId();
            finishedByAccount.merge(inventory.apply(category.apply(product)), w.getFinishedAmount(), BigDecimal::add);
        }
        finishedByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-完工入库", code, null, amount))); // i18n-ignore
        BigDecimal finished = Decimals.sum(finishedByAccount.values());
        lines.add(VoucherLine.credit(summary + "-完工入库", wip, null, finished)); // i18n-ignore
        // 销售成本、其他出入库
        Map<Long, BigDecimal> salesByAccount = new HashMap<>();
        // 主营业务成本按物料分类取科目
        Map<String, BigDecimal> salesByCategory = new HashMap<>();
        Map<Long, BigDecimal> otherByAccount = new HashMap<>();
        Map<Long, BigDecimal> projectByAccount = new HashMap<>();
        Map<Long, BigDecimal> outsourceByAccount = new HashMap<>();
        Map<Long, BigDecimal> serviceByAccount = new HashMap<>();
        Map<Long, BigDecimal> assemblyOutByAccount = new HashMap<>();
        materials.values().forEach(c -> {
            Long code = inventory.apply(category.apply(c.getMaterialId()));
            salesByAccount.merge(code, c.getSalesAmount(), BigDecimal::add);
            salesByCategory.merge(category.apply(c.getMaterialId()), c.getSalesAmount(), BigDecimal::add);
            // 其他出库中的项目领料、委外发料按单价拆出，其余（盘盈盘亏等）计入待处理财产损溢
            BigDecimal project = money(src.getProjectOut().getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
            BigDecimal outsource = money(src.getOutsourceOut().getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
            BigDecimal service = money(src.getServiceOut().getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
            BigDecimal assemblyOutAmount = money(src.getAssemblyOut().getOrDefault(c.getMaterialId(), BigDecimal.ZERO).multiply(c.getUnitCost()));
            assemblyOutByAccount.merge(code, assemblyOutAmount, BigDecimal::add);
            projectByAccount.merge(code, project, BigDecimal::add);
            outsourceByAccount.merge(code, outsource, BigDecimal::add);
            serviceByAccount.merge(code, service, BigDecimal::add);
            otherByAccount.merge(code, c.getOtherInAmount().subtract(c.getOtherOutAmount()).add(project).add(outsource).add(service)
                    .add(assemblyOutAmount), BigDecimal::add);
        });
        BigDecimal project = Decimals.sum(projectByAccount.values());
        lines.add(VoucherLine.debit(summary + "-项目领料", accountRules.accountId(AccountRole.CONTRACT_COST), null, project)); // i18n-ignore
        projectByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-项目领料", code, null, amount))); // i18n-ignore
        // 委外收货核销用料：借 委外件存货科目 / 贷 委托加工物资
        Map<Long, BigDecimal> consumedByAccount = new HashMap<>();
        result.outsourceConsumedAmount().forEach((product, amount) -> consumedByAccount.merge(inventory.apply(category.apply(product)), amount, BigDecimal::add));
        consumedByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-委外核销用料", code, null, amount))); // i18n-ignore
        BigDecimal consumedTotal = Decimals.sum(consumedByAccount.values());
        lines.add(VoucherLine.credit(summary + "-委外核销用料", accountRules.accountId(AccountRole.OUTSOURCE_MATERIAL), null, consumedTotal)); // i18n-ignore
        BigDecimal outsource = Decimals.sum(outsourceByAccount.values());
        lines.add(VoucherLine.debit(summary + "-委外发料", accountRules.accountId(AccountRole.OUTSOURCE_MATERIAL), null, outsource)); // i18n-ignore
        outsourceByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-委外发料", code, null, amount))); // i18n-ignore
        // 低值易耗品部门领用：按实际单价一次摊销，借 领用单的费用科目（领用部门），贷 周转材料；不再计入待处理财产损溢
        Map<Long, BigDecimal> consumeByAccount = new HashMap<>();
        for (ConsumeRow row : src.getConsumes()) {
            FinMaterialCost c = materials.get(row.materialId());
            if (c == null) {
                continue;
            }
            BigDecimal amount = money(row.qty().multiply(c.getUnitCost()));
            lines.add(VoucherLine.debit(summary + "-低值易耗品领用", row.accountId(), row.deptId(), amount)); // i18n-ignore
            consumeByAccount.merge(inventory.apply(category.apply(c.getMaterialId())), amount, BigDecimal::add);
        }
        consumeByAccount.forEach((code, amount) -> {
            lines.add(VoucherLine.credit(summary + "-低值易耗品领用", code, null, amount)); // i18n-ignore
            otherByAccount.merge(code, amount, BigDecimal::add);
        });
        // 组装拆卸：借 入库物料（组装的成套件、拆卸的子件）的存货科目 / 贷 出库物料的存货科目，尾差计入待处理财产损溢
        Map<Long, BigDecimal> assemblyInByAccount = new HashMap<>();
        for (Assembly a : src.getAssemblies()) {
            result.assemblyAmounts().get(a.no())
                    .forEach((m, amount) -> assemblyInByAccount.merge(inventory.apply(category.apply(m)), amount, BigDecimal::add));
        }
        assemblyInByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-组装拆卸", code, null, amount))); // i18n-ignore
        assemblyOutByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-组装拆卸", code, null, amount))); // i18n-ignore
        BigDecimal assemblyDiff = Decimals.sum(assemblyOutByAccount.values())
                .subtract(Decimals.sum(assemblyInByAccount.values()));
        lines.add(VoucherLine.debit(summary + "-组装拆卸尾差", accountRules.accountId(AccountRole.PENDING_LOSS), null, assemblyDiff)); // i18n-ignore
        BigDecimal service = Decimals.sum(serviceByAccount.values());
        lines.add(VoucherLine.debit(summary + "-售后备件", accountRules.accountId(AccountRole.SELLING_EXPENSE), null, service)); // i18n-ignore
        serviceByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-售后备件", code, null, amount))); // i18n-ignore
        salesByCategory.forEach((cat, amount) -> lines.add(
                VoucherLine.debit(summary + "-销售成本", accountRules.accountId(AccountRole.COST_OF_SALES, cat), null, amount))); // i18n-ignore
        salesByAccount.forEach((code, amount) -> lines.add(VoucherLine.credit(summary + "-销售成本", code, null, amount))); // i18n-ignore
        BigDecimal other = Decimals.sum(otherByAccount.values());
        otherByAccount.forEach((code, amount) -> lines.add(VoucherLine.debit(summary + "-其他出入库", code, null, amount))); // i18n-ignore
        otherOffsets(src, materials, other).forEach((account, amount) -> lines.add(VoucherLine.credit(summary + "-其他出入库", account, null, amount))); // i18n-ignore
        return new Lines(lines.stream().anyMatch(l -> !l.isZero()) ? lines : List.of(), overheadVariance);
    }

    /**
     * 其他出入库的对方科目（贷方金额，负数记借方）：填写了收发类别的按「数量 × 本期单价」拆出，
     * 出库 借 出库类别的对方科目、入库 贷 入库类别的对方科目（凭证科目设置，未设置时同待处理财产损溢）；
     * 其余（盘盈盘亏、未填类别、计价差额等）计入待处理财产损溢，存货科目的金额不变
     *
     * @param other 其他出入库使存货增加的净额
     */
    private Map<Long, BigDecimal> otherOffsets(CostSource src, Map<Long, FinMaterialCost> materials, BigDecimal other) {
        Long pendingLoss = accountRules.accountId(AccountRole.PENDING_LOSS);
        Map<Long, BigDecimal> offsets = new LinkedHashMap<>();
        offsets.put(pendingLoss, other);
        Map<String, Long> accounts = new HashMap<>();
        BiConsumer<Map<Long, Map<String, BigDecimal>>, AccountRole> split = (byMaterial, role) -> byMaterial.forEach((m, byCategory) -> {
            FinMaterialCost c = materials.get(m);
            if (c == null) {
                return;
            }
            byCategory.forEach((cat, qty) -> {
                // 入库贷方为正，出库记借方（贷方为负）
                BigDecimal amount = money(qty.multiply(c.getUnitCost()));
                BigDecimal credit = role == AccountRole.OTHER_IN_OFFSET ? amount : amount.negate();
                Long account = accounts.computeIfAbsent(role.name() + ":" + cat, k -> accountRules.accountId(role, cat));
                offsets.merge(account, credit, BigDecimal::add);
                offsets.merge(pendingLoss, credit.negate(), BigDecimal::add);
            });
        });
        split.accept(src.getCategoryIn(), AccountRole.OTHER_IN_OFFSET);
        split.accept(src.getCategoryOut(), AccountRole.OTHER_OUT_OFFSET);
        return offsets;
    }
}
