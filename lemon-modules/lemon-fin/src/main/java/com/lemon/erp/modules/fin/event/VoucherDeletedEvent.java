package com.lemon.erp.modules.fin.event;

import java.util.List;

/**
 * 凭证已删除（与删除同一事务）。下游模块（工资、项目等）生成的凭证被删除时，监听此事件解除来源单据与凭证的关联，以便重新生成
 */
public record VoucherDeletedEvent(List<Long> voucherIds) {
}
