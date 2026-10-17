package com.lemon.erp.modules.fin.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报表行的构成科目（末级科目），用于从报表数字追溯到科目和明细账
 *
 * @param values 与报表列一一对应，按报表行的方向取数
 */
public record FinancialRowAccount(Long accountId, String code, String name, List<BigDecimal> values) {
}
