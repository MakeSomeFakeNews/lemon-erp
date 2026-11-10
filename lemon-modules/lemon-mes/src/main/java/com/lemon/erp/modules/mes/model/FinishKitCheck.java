package com.lemon.erp.modules.mes.model;

import com.lemon.erp.framework.setting.OptionEnum;

/**
 * 完工入库的领料校验方式（业务参数 mes.finish.kitCheck），label 为消息键：
 * 累计完工入库数量超过「按已领料可支持的齐套数量」时如何处理
 */
public enum FinishKitCheck implements OptionEnum {
    /** 不校验 */
    NONE("mes.label.kitCheckNone"),
    /** 提示：界面提示后仍可入库 */
    WARN("mes.label.kitCheckWarn"),
    /** 禁止：超出时不能入库 */
    BLOCK("mes.label.kitCheckBlock");

    private final String label;

    FinishKitCheck(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
