package com.lemon.erp.modules.fin.model;

import java.math.BigDecimal;

/**
 * 业务生成凭证的一条分录（借方、贷方只填一边）
 *
 * @param projectId   辅助核算：项目
 * @param partnerType 辅助核算：往来单位类型 CUSTOMER 客户 / SUPPLIER 供应商
 * @param partnerId   辅助核算：往来单位
 */
public record VoucherLine(String summary, Long accountId, Long deptId, BigDecimal debit, BigDecimal credit,
                          Long projectId, String partnerType, Long partnerId) {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String SUPPLIER = "SUPPLIER";

    public VoucherLine(String summary, Long accountId, Long deptId, BigDecimal debit, BigDecimal credit) {
        this(summary, accountId, deptId, debit, credit, null, null, null);
    }

    public static VoucherLine debit(String summary, Long accountId, Long deptId, BigDecimal amount) {
        return amount.signum() >= 0 ? new VoucherLine(summary, accountId, deptId, amount, BigDecimal.ZERO)
                : new VoucherLine(summary, accountId, deptId, BigDecimal.ZERO, amount.negate());
    }

    /** 金额为负数时记到另一方（红字冲销按反方向记账） */
    public static VoucherLine credit(String summary, Long accountId, Long deptId, BigDecimal amount) {
        return amount.signum() >= 0 ? new VoucherLine(summary, accountId, deptId, BigDecimal.ZERO, amount)
                : new VoucherLine(summary, accountId, deptId, amount.negate(), BigDecimal.ZERO);
    }

    public VoucherLine withProject(Long project) {
        return new VoucherLine(summary, accountId, deptId, debit, credit, project, partnerType, partnerId);
    }

    public VoucherLine withPartner(String type, Long id) {
        return new VoucherLine(summary, accountId, deptId, debit, credit, projectId, id == null ? null : type, id);
    }

    public boolean isZero() {
        return debit.signum() == 0 && credit.signum() == 0;
    }

    /** 合并分录时的辅助核算是否相同 */
    public boolean sameAux(VoucherLine o) {
        return java.util.Objects.equals(deptId, o.deptId) && java.util.Objects.equals(projectId, o.projectId)
                && java.util.Objects.equals(partnerType, o.partnerType) && java.util.Objects.equals(partnerId, o.partnerId);
    }
}
