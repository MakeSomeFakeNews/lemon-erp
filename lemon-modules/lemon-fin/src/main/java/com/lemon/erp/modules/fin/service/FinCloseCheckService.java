package com.lemon.erp.modules.fin.service;

import com.lemon.erp.modules.fin.model.AccountRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.model.AssetStatus;
import com.lemon.erp.modules.fin.entity.FinBankStatement;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinCostRun;
import com.lemon.erp.modules.fin.entity.FinDepreciation;
import com.lemon.erp.modules.fin.entity.FinPeriod;
import com.lemon.erp.modules.fin.entity.FinSettlement;
import com.lemon.erp.modules.fin.entity.FinVoucher;
import com.lemon.erp.modules.fin.mapper.FinAssetMapper;
import com.lemon.erp.modules.fin.mapper.FinBankStatementMapper;
import com.lemon.erp.modules.fin.mapper.FinBillMapper;
import com.lemon.erp.modules.fin.mapper.FinCostRunMapper;
import com.lemon.erp.modules.fin.mapper.FinDepreciationMapper;
import com.lemon.erp.modules.fin.mapper.FinPeriodMapper;
import com.lemon.erp.modules.fin.mapper.FinSettlementMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherEntryMapper;
import com.lemon.erp.modules.fin.mapper.FinVoucherMapper;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 结账前检查：错误项（ERROR）不处理不能结账，提示项（WARN）建议处理
 */
@Service
@RequiredArgsConstructor
public class FinCloseCheckService {

    private final FinPeriodMapper periodMapper;
    private final FinVoucherMapper voucherMapper;
    private final FinVoucherEntryMapper entryMapper;
    private final FinBillMapper billMapper;
    private final FinSettlementMapper settlementMapper;
    private final FinCostRunMapper costRunMapper;
    private final FinDepreciationMapper depreciationMapper;
    private final FinAssetMapper assetMapper;
    private final FinBankStatementMapper statementMapper;
    private final FinTaxService taxService;
    private final FinAccountService accountService;
    private final FinAccountRuleService accountRules;
    private final FinReportService reportService;

    /** @param level OK 通过 / WARN 提示 / ERROR 不能结账 */
    public record CheckItem(String name, String level, String message) {
    }

    public List<CheckItem> check(int period) {
        YearMonth ym = FinConstants.toYearMonth(period);
        LocalDate begin = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<CheckItem> items = new ArrayList<>();

        FinPeriod earlierOpen = periodMapper.selectOne(Wrappers.<FinPeriod>lambdaQuery().lt(FinPeriod::getPeriod, period)
                .eq(FinPeriod::getStatus, FinPeriodService.OPEN).orderByAsc(FinPeriod::getPeriod).last("LIMIT 1"));
        items.add(earlierOpen == null ? ok("之前的期间已结账") : error("之前的期间已结账", earlierOpen.getPeriod() + " 期间还未结账"));

        long unposted = voucherMapper.selectCount(Wrappers.<FinVoucher>lambdaQuery().eq(FinVoucher::getPeriod, period)
                .ne(FinVoucher::getStatus, VoucherStatus.POSTED.name()));
        items.add(unposted == 0 ? ok("凭证全部记账") : error("凭证全部记账", unposted + " 张凭证未审核或未记账"));

        boolean balanced = reportService.trialBalance(period, period, false).balanced();
        items.add(balanced ? ok("试算平衡") : error("试算平衡", "期初、本期发生或期末余额借贷不平衡"));

        long bills = billMapper.selectCount(Wrappers.<FinBill>lambdaQuery().between(FinBill::getBillDate, begin, end)
                .isNull(FinBill::getVoucherId).ne(FinBill::getSourceType, "MANUAL"));
        items.add(bills == 0 ? ok("应收应付单已生成凭证") : warn("应收应付单已生成凭证", bills + " 张应收应付单未生成凭证"));
        long settles = settlementMapper.selectCount(Wrappers.<FinSettlement>lambdaQuery().between(FinSettlement::getSettleDate, begin, end)
                .isNull(FinSettlement::getVoucherId).eq(FinSettlement::getStatus, "APPROVED"));
        items.add(settles == 0 ? ok("收付款单已生成凭证") : warn("收付款单已生成凭证", settles + " 张收付款单未生成凭证"));

        FinCostRun run = costRunMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period).last("LIMIT 1"));
        items.add(run == null ? warn("成本核算", "本期还没有做成本核算") : run.getVoucherId() == null ? warn("成本核算", "成本结转凭证未生成")
                : ok("成本核算"));

        boolean hasAssets = assetMapper.exists(Wrappers.<FinAsset>lambdaQuery().in(FinAsset::getStatus, AssetStatus.IN_USE, AssetStatus.IDLE)
                .lt(FinAsset::getUseDate, begin));
        boolean depreciated = depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().eq(FinDepreciation::getPeriod, period));
        items.add(!hasAssets || depreciated ? ok("计提折旧") : warn("计提折旧", "本期还没有计提折旧"));

        boolean output = entryMapper.sumBalanceUpTo(accountRules.accountId(AccountRole.OUTPUT_VAT), period).signum() < 0;
        items.add(!output || taxService.transferred(period) ? ok("增值税结转") : warn("增值税结转", "销项税额还未结转，未计提附加税费"));

        long unmatched = statementMapper.selectCount(Wrappers.<FinBankStatement>lambdaQuery().between(FinBankStatement::getTxnDate, begin, end)
                .isNull(FinBankStatement::getEntryId));
        items.add(unmatched == 0 ? ok("银行对账") : warn("银行对账", unmatched + " 笔银行流水未勾对"));
        return items;
    }

    private static CheckItem ok(String name) {
        return new CheckItem(name, "OK", null);
    }

    private static CheckItem warn(String name, String message) {
        return new CheckItem(name, "WARN", message);
    }

    private static CheckItem error(String name, String message) {
        return new CheckItem(name, "ERROR", message);
    }
}
