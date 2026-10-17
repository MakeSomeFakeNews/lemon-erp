package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.service.FinBankService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 导入银行对账单（前端解析 Excel 后按行提交）
 */
public record BankStatementImport(@NotNull Long accountId, @NotEmpty List<FinBankService.StatementRow> rows) {
}
