package com.lemon.erp.modules.fin.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 业务生成凭证的一条分录（借方、贷方只填一边）。辅助核算各项可同时填写
 *
 * @param deptId     辅助核算：部门
 * @param projectId  辅助核算：项目
 * @param customerId 辅助核算：客户
 * @param supplierId 辅助核算：供应商
 * @param materialId 辅助核算：存货（物料）
 */
public record VoucherLine(String summary, Long accountId, Long deptId, BigDecimal debit, BigDecimal credit,
                          Long projectId, Long customerId, Long supplierId, Long materialId) {

    public VoucherLine(String summary, Long accountId, Long deptId, BigDecimal debit, BigDecimal credit) {
        this(summary, accountId, deptId, debit, credit, null, null, null, null);
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

    public VoucherLine withSummary(String s) {
        return new VoucherLine(s, accountId, deptId, debit, credit, projectId, customerId, supplierId, materialId);
    }

    public VoucherLine withAmounts(BigDecimal d, BigDecimal c) {
        return new VoucherLine(summary, accountId, deptId, d, c, projectId, customerId, supplierId, materialId);
    }

    public VoucherLine withDept(Long dept) {
        return new VoucherLine(summary, accountId, dept, debit, credit, projectId, customerId, supplierId, materialId);
    }

    public VoucherLine withProject(Long project) {
        return new VoucherLine(summary, accountId, deptId, debit, credit, project, customerId, supplierId, materialId);
    }

    public VoucherLine withCustomer(Long customer) {
        return new VoucherLine(summary, accountId, deptId, debit, credit, projectId, customer, supplierId, materialId);
    }

    public VoucherLine withSupplier(Long supplier) {
        return new VoucherLine(summary, accountId, deptId, debit, credit, projectId, customerId, supplier, materialId);
    }

    public VoucherLine withMaterial(Long material) {
        return new VoucherLine(summary, accountId, deptId, debit, credit, projectId, customerId, supplierId, material);
    }

    public boolean isZero() {
        return debit.signum() == 0 && credit.signum() == 0;
    }

    /** 合并分录时的辅助核算是否相同 */
    public boolean sameAux(VoucherLine o) {
        return Objects.equals(deptId, o.deptId) && Objects.equals(projectId, o.projectId) && Objects.equals(customerId, o.customerId)
                && Objects.equals(supplierId, o.supplierId) && Objects.equals(materialId, o.materialId);
    }

    /** 已填写的辅助核算项目 */
    public boolean has(AuxItem item) {
        return switch (item) {
            case DEPT -> deptId != null;
            case CUSTOMER -> customerId != null;
            case SUPPLIER -> supplierId != null;
            case PROJECT -> projectId != null;
            case MATERIAL -> materialId != null;
        };
    }
}
