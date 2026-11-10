package com.lemon.erp.modules.scm.model;

import com.lemon.erp.framework.setting.OptionEnum;

/**
 * RFM 分群的高低判断方式（业务参数 scm.rfm.mode），label 为消息键
 */
public enum RfmMode implements OptionEnum {
    /** 固定阈值：最近 N 天内订过货、每月订货不少于 N 次、平均每次订货金额不少于 N 元为「高」 */
    THRESHOLD("scm.label.rfmModeThreshold"),
    /** 平均分：R、F、M 各按五分位打 1–5 分，不低于全体平均分为「高」（随客户整体水平自动调整） */
    AVERAGE("scm.label.rfmModeAverage");

    private final String label;

    RfmMode(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
