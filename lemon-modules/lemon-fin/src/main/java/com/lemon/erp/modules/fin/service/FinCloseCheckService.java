package com.lemon.erp.modules.fin.service;

import com.lemon.erp.common.i18n.Messages;
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
        items.add(earlierOpen == null ? ok("fin.label.earlierPeriodsClosed") : error("fin.label.earlierPeriodsClosed", Messages.get("fin.text.periodHasNotClosed", earlierOpen.getPeriod())));

        long unposted = voucherMapper.selectCount(Wrappers.<FinVoucher>lambdaQuery().eq(FinVoucher::getPeriod, period)
                .ne(FinVoucher::getStatus, VoucherStatus.POSTED.name()));
        items.add(unposted == 0 ? ok("fin.label.allVouchersPosted") : error("fin.label.allVouchersPosted", Messages.get("fin.text.voucherSNotApprovedNotPosted", unposted)));

        boolean balanced = reportService.trialBalance(period, period, false).balanced();
        items.add(balanced ? ok("fin.label.trialBalance") : error("fin.label.trialBalance", Messages.get("fin.text.debitsCreditsOpeningBalancePeriodActivity")));

        long bills = billMapper.selectCount(Wrappers.<FinBill>lambdaQuery().between(FinBill::getBillDate, begin, end)
                .isNull(FinBill::getVoucherId).ne(FinBill::getSourceType, "MANUAL"));
        items.add(bills == 0 ? ok("fin.label.vouchersGeneratedReceivablesPayables") : warn("fin.label.vouchersGeneratedReceivablesPayables", Messages.get("fin.text.receivablePayableDocumentSWithoutVouchers", bills)));
        long settles = settlementMapper.selectCount(Wrappers.<FinSettlement>lambdaQuery().between(FinSettlement::getSettleDate, begin, end)
                .isNull(FinSettlement::getVoucherId).eq(FinSettlement::getStatus, "APPROVED"));
        items.add(settles == 0 ? ok("fin.label.vouchersGeneratedReceiptsPayments") : warn("fin.label.vouchersGeneratedReceiptsPayments", Messages.get("fin.text.receiptPaymentDocumentSWithoutVouchers", settles)));

        FinCostRun run = costRunMapper.selectOne(Wrappers.<FinCostRun>lambdaQuery().eq(FinCostRun::getPeriod, period).last("LIMIT 1"));
        items.add(run == null ? warn("fin.label.costAccounting", Messages.get("fin.text.costAccountingHasNotRunPeriod")) : run.getVoucherId() == null ? warn("fin.label.costAccounting", Messages.get("fin.text.costTransferVoucherHasNotGenerated"))
                : ok("fin.label.costAccounting"));

        boolean hasAssets = assetMapper.exists(Wrappers.<FinAsset>lambdaQuery().in(FinAsset::getStatus, AssetStatus.IN_USE, AssetStatus.IDLE)
                .lt(FinAsset::getUseDate, begin));
        boolean depreciated = depreciationMapper.exists(Wrappers.<FinDepreciation>lambdaQuery().eq(FinDepreciation::getPeriod, period));
        items.add(!hasAssets || depreciated ? ok("fin.label.depreciation") : warn("fin.label.depreciation", Messages.get("fin.text.depreciationHasNotAccruedPeriod")));

        boolean output = entryMapper.sumBalanceUpTo(accountRules.accountId(AccountRole.OUTPUT_VAT), period).signum() < 0;
        items.add(!output || taxService.transferred(period) ? ok("fin.label.vatTransfer") : warn("fin.label.vatTransfer", Messages.get("fin.text.outputVatHasNotTransferredSurtaxes")));

        long unmatched = statementMapper.selectCount(Wrappers.<FinBankStatement>lambdaQuery().between(FinBankStatement::getTxnDate, begin, end)
                .isNull(FinBankStatement::getEntryId));
        items.add(unmatched == 0 ? ok("fin.label.bankReconciliation") : warn("fin.label.bankReconciliation", Messages.get("fin.text.bankTransactionSNotMatched", unmatched)));
        return items;
    }

    /** 检查项名称返回给前端显示，按请求语言翻译；message 已在调用处翻译 */
    private static CheckItem ok(String name) {
        return new CheckItem(Messages.get(name), "OK", null);
    }

    private static CheckItem warn(String name, String message) {
        return new CheckItem(Messages.get(name), "WARN", message);
    }

    private static CheckItem error(String name, String message) {
        return new CheckItem(Messages.get(name), "ERROR", message);
    }
}
