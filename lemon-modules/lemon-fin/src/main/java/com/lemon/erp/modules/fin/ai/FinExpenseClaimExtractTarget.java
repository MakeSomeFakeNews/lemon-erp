package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiExtractTarget;
import com.lemon.erp.framework.ai.AiPrefill;
import com.lemon.erp.framework.ai.AiPrefillBuilder;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.modules.fin.entity.FinAccount;
import com.lemon.erp.modules.fin.service.FinAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 智能录入：费用报销（发票、出租车票、火车票、行程单等票据照片 → 报销明细）。
 * 费用科目只能从启用的末级费用科目中选（生产成本 5101、销售费用 6601、管理费用 6602、财务费用 6603 下，与报销页面一致），
 * 科目列表作为枚举交给模型；每张票据一行，金额为价税合计，发票号码、销售方写入明细说明
 */
@Component
@Order(60) // 目标列表中的顺序
@RequiredArgsConstructor
public class FinExpenseClaimExtractTarget implements AiExtractTarget {

    /** 报销可选的费用科目范围（与前端报销页面的 EXPENSE_PREFIX 一致） */
    static final List<String> EXPENSE_PREFIXES = List.of("5101", "6601", "6602", "6603");
    /** 报销明细的行数上限（与 ClaimRequest 一致） */
    private static final int MAX_ITEMS = 100;
    /** 科目超过该数量时不作为枚举（避免提示词过长），由名称匹配 */
    private static final int MAX_ENUM_ACCOUNTS = 120;

    private final FinAccountService accountService;

    @Override
    public String key() {
        return "fin.expense-claim";
    }

    @Override
    public String labelKey() {
        return "fin.label.expenseClaim";
    }

    @Override
    public String permission() {
        return "fin:claim:add";
    }

    @Override
    public Map<String, Object> schema() {
        List<String> accounts = expenseAccounts().stream().map(FinExpenseClaimExtractTarget::accountLabel).toList();
        AiSchema account = accounts.isEmpty() || accounts.size() > MAX_ENUM_ACCOUNTS
                ? AiSchema.string("fin.ai.extractAccount").maxLength(64)
                : AiSchema.enumOf("fin.ai.extractAccount", accounts.toArray(String[]::new));
        AiSchema item = AiSchema.object()
                .prop("expenseDate", AiSchema.date("fin.ai.extractExpenseDate"), false)
                .prop("account", account, false)
                .prop("amount", AiSchema.number("fin.ai.extractAmount"), false)
                .prop("invoiceNo", AiSchema.string("fin.ai.extractInvoiceNo").maxLength(32), false)
                .prop("seller", AiSchema.string("fin.ai.extractSeller").maxLength(128), false)
                .prop("description", AiSchema.string("fin.ai.extractDescription").maxLength(200), false);
        return AiSchema.object()
                .prop("reason", AiSchema.string("fin.ai.extractReason").maxLength(255), false)
                .prop("items", AiSchema.array("fin.ai.extractItems", item).maxItems(MAX_ITEMS), true)
                .build();
    }

    @Override
    public String instructionKey() {
        return "fin.ai.extractInstruction";
    }

    @Override
    public AiPrefill prefill(Map<String, Object> ex) {
        AiPrefillBuilder b = new AiPrefillBuilder();
        b.put("remark", cut(AiPrefillBuilder.str(ex, "reason"), 255));
        List<FinAccount> accounts = expenseAccounts();
        List<Map<String, Object>> rows = b.list(ex, "items", "items", MAX_ITEMS);
        if (rows.isEmpty()) {
            b.warn("items", null, "fin.ai.warnNoItems");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String prefix = "items[" + i + "].";
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("expenseDate", b.date(prefix + "expenseDate", AiPrefillBuilder.str(row, "expenseDate")));
            String accountText = AiPrefillBuilder.str(row, "account");
            FinAccount account = b.match(prefix + "accountId", null, accountText, Messages.get("fin.ai.labelAccount"),
                    t -> accounts.stream().filter(a -> t.equals(accountLabel(a)) || t.equals(a.getCode())).findFirst().orElse(null),
                    t -> accounts.stream().filter(a -> a.getName().equals(t) || t.contains(a.getName())).toList(),
                    FinExpenseClaimExtractTarget::accountLabel);
            if (account != null) {
                item.put("accountId", account.getId());
            } else {
                b.required(prefix + "accountId", accountText, Messages.get("fin.ai.labelAccount"));
                item.put("aiText", accountText);
            }
            BigDecimal amount = b.nonNegative(prefix + "amount", AiPrefillBuilder.dec(row, "amount"));
            if (amount != null && amount.signum() == 0) {
                b.warn(prefix + "amount", "0", "fin.ai.warnAmountZero");
                amount = null;
            }
            b.required(prefix + "amount", amount, Messages.get("fin.ai.labelAmount"));
            item.put("amount", amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP));
            item.put("remark", cut(description(row), 255));
            item.values().removeIf(Objects::isNull);
            items.add(item);
        }
        b.put("items", items);
        return b.build();
    }

    /** 明细说明：摘要 + 发票号码 + 销售方 */
    private static String description(Map<String, Object> row) {
        List<String> parts = new ArrayList<>();
        String desc = AiPrefillBuilder.str(row, "description");
        String invoiceNo = AiPrefillBuilder.str(row, "invoiceNo");
        String seller = AiPrefillBuilder.str(row, "seller");
        if (desc != null) {
            parts.add(desc);
        }
        if (invoiceNo != null) {
            parts.add(Messages.get("fin.ai.invoiceNoText", invoiceNo));
        }
        if (seller != null) {
            parts.add(seller);
        }
        return parts.isEmpty() ? null : String.join(Messages.get("fin.ai.descriptionSep"), parts);
    }

    /** 启用的末级费用科目 */
    private List<FinAccount> expenseAccounts() {
        return accountService.leafOptions().stream()
                .filter(a -> FinAccountService.inScope(a.getCode(), EXPENSE_PREFIXES))
                .toList();
    }

    private static String accountLabel(FinAccount a) {
        return a.getCode() + " " + a.getName();
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public String route() {
        return "/fin/claim";
    }

    @Override
    public String form() {
        return "expense-claim";
    }
}
