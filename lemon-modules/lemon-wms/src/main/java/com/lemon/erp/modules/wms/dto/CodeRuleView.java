package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.framework.code.DocNoCatalog.DateFormat;
import com.lemon.erp.framework.code.DocNoCatalog.ResetCycle;

/**
 * 编码规则（生效中的规则：租户修改过的，或默认规则）
 *
 * @param customized 是否修改过默认规则
 * @param example    按当前规则生成的示例编号（流水为 1）
 */
public record CodeRuleView(String key, String name, String module, String prefix, DateFormat dateFormat, int seqLength,
                           ResetCycle resetCycle, boolean customized, String example) {
}
