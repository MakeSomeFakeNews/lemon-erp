package com.lemon.erp.modules.scm.dto;

import java.util.List;

/**
 * 承运商下拉：采购运费（本公司承担）、发运单的运费结算供应商
 *
 * @param marked  是否已有标记为物流公司的启用供应商；没有时 options 为全部供应商，页面提示去供应商资料中标记
 * @param options 选项（按编码或名称搜索，最多 50 条）
 */
public record CarrierOptions(boolean marked, List<PartnerOption> options) {
}
