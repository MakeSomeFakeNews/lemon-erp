package com.lemon.erp.modules.scm.service;

import com.lemon.erp.common.util.Decimals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 运费分摊（纯计算）：采购入库的运费按各行不含税金额的比例分摊到各行，计入存货成本；
 * 各行 round2(运费 × 行金额 / 合计)，尾差放在金额最大的一行，合计精确等于运费。金额合计为 0（全是赠品）时按数量分摊
 */
public final class ScmFreights {

    private ScmFreights() {
    }

    /**
     * @param freight 运费（不小于 0）
     * @param weights 各行权重（不含税金额）
     * @param qtys    各行数量，权重合计为 0 时按数量分摊
     * @return 各行分摊的运费
     */
    public static List<BigDecimal> allocate(BigDecimal freight, List<BigDecimal> weights, List<BigDecimal> qtys) {
        BigDecimal total = Decimals.money(freight);
        List<BigDecimal> result = new ArrayList<>(weights.size());
        if (weights.isEmpty()) {
            return result;
        }
        BigDecimal sum = Decimals.sum(weights);
        List<BigDecimal> w = sum.signum() > 0 ? weights : qtys;
        BigDecimal base = sum.signum() > 0 ? sum : Decimals.sum(qtys);
        int largest = 0;
        for (int i = 1; i < w.size(); i++) {
            if (w.get(i).compareTo(w.get(largest)) > 0) {
                largest = i;
            }
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < w.size(); i++) {
            BigDecimal share = i == largest || base.signum() == 0 ? BigDecimal.ZERO
                    : total.multiply(w.get(i)).divide(base, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(share);
            result.add(share);
        }
        result.set(largest, total.subtract(allocated));
        return result;
    }
}
