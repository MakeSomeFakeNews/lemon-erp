package com.lemon.erp.modules.bi.model;

/**
 * RFM 客户分群：最近购买（R）、购买频次（F）、购买金额（M）的得分不低于全体平均分为「高」，按高低组合分为八类
 */
public enum RfmSegment {
    /** 重要价值：R 高 F 高 M 高 */
    IMPORTANT_VALUE,
    /** 重要发展：R 高 F 低 M 高 */
    IMPORTANT_DEVELOP,
    /** 重要保持：R 低 F 高 M 高 */
    IMPORTANT_KEEP,
    /** 重要挽留：R 低 F 低 M 高 */
    IMPORTANT_RETAIN,
    /** 一般价值：R 高 F 高 M 低 */
    GENERAL_VALUE,
    /** 一般发展：R 高 F 低 M 低 */
    GENERAL_DEVELOP,
    /** 一般保持：R 低 F 高 M 低 */
    GENERAL_KEEP,
    /** 一般挽留：R 低 F 低 M 低 */
    GENERAL_RETAIN;

    public static RfmSegment of(boolean r, boolean f, boolean m) {
        if (m) {
            return r ? (f ? IMPORTANT_VALUE : IMPORTANT_DEVELOP) : (f ? IMPORTANT_KEEP : IMPORTANT_RETAIN);
        }
        return r ? (f ? GENERAL_VALUE : GENERAL_DEVELOP) : (f ? GENERAL_KEEP : GENERAL_RETAIN);
    }
}
