package com.lemon.erp.modules.scm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 可承诺量
 *
 * @param available     可用库存（库存 - 冻结）
 * @param reserved      已预留
 * @param free          空闲库存 = 可用 - 预留
 * @param atp           现在可以承诺的数量：未来各时点的累计余额的最小值（不影响已有的需求）
 * @param promiseDate   请求数量可以承诺的最早日期（传了数量时计算；不能满足时为空）
 * @param lines         按日期排列的未来供需及累计余额
 */
public record AtpResult(BigDecimal available, BigDecimal reserved, BigDecimal free, BigDecimal atp, LocalDate promiseDate,
                        List<Line> lines) {

    public record Line(LocalDate date, String type, String docNo, BigDecimal qty, BigDecimal balance) {
    }
}
