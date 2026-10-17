package com.lemon.erp.modules.mes.quality;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.mes.model.InspectionLevel;

import java.math.BigDecimal;
import java.util.List;

/**
 * GB/T 2828.1（等同 ISO 2859-1）正常检验一次抽样方案：
 * 按批量和检验水平查样本量字码（表 1），再按字码和 AQL 查样本量与接收数 Ac / 拒收数 Re（表 2-A）。
 * <p>
 * 表 2-A 中每个 AQL 列的结构相同：在某一字码处为 0/1，下一行为↑（沿用上方 0/1 方案），再下一行为↓（沿用下方 1/2 方案），
 * 之后依次为 1/2、2/3、3/4、5/6、7/8、10/11、14/15、21/22；该行以上为↓（沿用 0/1 方案）。
 * 各 AQL 的 0/1 所在字码依次错开一行，因此只需记录 0/1 所在的字码即可还原整列。
 */
public final class AqlSampling {

    /** 样本量字码及对应样本量 */
    static final List<String> CODES = List.of("A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R");
    static final int[] SAMPLE_SIZES = {2, 3, 5, 8, 13, 20, 32, 50, 80, 125, 200, 315, 500, 800, 1250, 2000};

    /** 批量范围的上限（含），最后一档为 500001 以上 */
    private static final long[] LOT_UPPER = {8, 15, 25, 50, 90, 150, 280, 500, 1200, 3200, 10000, 35000, 150000, 500000, Long.MAX_VALUE};

    /** 表 1：各检验水平在各批量档的字码（行为检验水平，顺序同 InspectionLevel） */
    private static final String[] LEVEL_CODES = {
            "AAAABBBBCCCCDDD", // S-1
            "AAABBBCCCDDDEEE", // S-2
            "AABBCCDDEEFFGGH", // S-3
            "AABCCDEEFGGHJJK", // S-4
            "AABCCDEFGHJKLMN", // I
            "ABCDEFGHJKLMNPQ", // II
            "BCDEFGHJKLMNPQR"  // III
    };

    /** 表 2-A 中可选的 AQL 及其 0/1 所在字码 */
    static final List<BigDecimal> AQLS = List.of(new BigDecimal("0.040"), new BigDecimal("0.065"), new BigDecimal("0.10"),
            new BigDecimal("0.15"), new BigDecimal("0.25"), new BigDecimal("0.40"), new BigDecimal("0.65"), new BigDecimal("1.0"),
            new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("4.0"), new BigDecimal("6.5"));
    private static final String[] ZERO_ACCEPT_CODES = {"M", "L", "K", "J", "H", "G", "F", "E", "D", "C", "B", "A"};

    /** 0/1 之后（隔一个↑和一个↓）的接收数序列 */
    private static final int[] ACCEPTS = {1, 2, 3, 5, 7, 10, 14, 21};

    private AqlSampling() {
    }

    /**
     * @param code       样本量字码（查表得到的，不是最终使用的方案所在字码）
     * @param sampleSize 样本量；大于批量时为全检（等于批量）
     * @param accept     接收数 Ac：不合格品数 ≤ Ac 接收
     * @param reject     拒收数 Re：不合格品数 ≥ Re 拒收
     */
    public record Plan(String code, int sampleSize, int accept, int reject) {
    }

    public static String codeLetter(long lotSize, InspectionLevel level) {
        if (lotSize < 2) {
            throw new BizException("mes.error.lotSizeMustLeast");
        }
        int band = 0;
        while (lotSize > LOT_UPPER[band]) {
            band++;
        }
        return String.valueOf(LEVEL_CODES[level.ordinal()].charAt(band));
    }

    public static Plan plan(long lotSize, InspectionLevel level, BigDecimal aql) {
        int a = indexOfAql(aql);
        String code = codeLetter(lotSize, level);
        int row = CODES.indexOf(code);
        int zero = CODES.indexOf(ZERO_ACCEPT_CODES[a]);
        int offset = row - zero;
        int planRow;
        int accept;
        if (offset <= 1) {
            // 0/1 及其上方的↓、下一行的↑：都使用 0/1 方案
            planRow = zero;
            accept = 0;
        } else if (offset == 2) {
            // ↓：使用下一行的 1/2 方案
            planRow = zero + 3;
            accept = 1;
        } else {
            int step = Math.min(offset - 3, ACCEPTS.length - 1);
            planRow = Math.min(zero + 3 + step, CODES.size() - 1);
            accept = ACCEPTS[step];
        }
        int n = SAMPLE_SIZES[planRow];
        return new Plan(code, (int) Math.min(n, lotSize), accept, accept + 1);
    }

    private static int indexOfAql(BigDecimal aql) {
        for (int i = 0; i < AQLS.size(); i++) {
            if (AQLS.get(i).compareTo(aql) == 0) {
                return i;
            }
        }
        throw new BizException("mes.error.unsupportedAqlAvailable", aql.stripTrailingZeros().toPlainString());
    }
}
