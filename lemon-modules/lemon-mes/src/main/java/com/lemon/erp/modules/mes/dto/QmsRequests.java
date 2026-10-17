package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.mes.model.CapaSource;
import com.lemon.erp.modules.mes.model.Disposition;
import com.lemon.erp.modules.mes.model.InspectionLevel;
import com.lemon.erp.modules.mes.model.ItemMethod;
import com.lemon.erp.modules.mes.model.NcrSource;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.model.SamplingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 质量管理的请求
 */
public final class QmsRequests {

    private QmsRequests() {
    }

    /**
     * 检验方案：物料与物料分类二选一（物料优先匹配）
     *
     * @param fixedQty   固定抽样数量（FIXED）
     * @param acceptQty  固定抽样的允收不良数（FIXED）
     * @param aql        AQL（AQL 抽样）
     */
    public record Plan(
            @NotBlank(message = "{mes.valid.enterSchemeName}") @Size(max = 64, message = "{mes.valid.schemeNameCannotExceedCharacters}") String name,
            @NotNull(message = "{mes.valid.selectInspectionType}") QcType inspType,
            Long materialId,
            @Size(max = 32) String materialCategory,
            @NotNull(message = "{mes.valid.selectSamplingMode}") SamplingMode samplingMode,
            @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.sampleQuantityMustGreater2}") BigDecimal fixedQty,
            @Min(value = 0, message = "{mes.valid.acceptanceNumberCannotLess}") Integer acceptQty,
            BigDecimal aql,
            InspectionLevel inspectionLevel,
            EnableStatus status,
            @Size(max = 255) String remark,
            @NotEmpty(message = "{mes.valid.addInspectionItems}") @Size(max = 100, message = "{mes.valid.inspectionItemsCannotExceed}") List<@Valid @NotNull PlanItem> items) {
    }

    public record PlanItem(
            @NotBlank(message = "{mes.valid.enterInspectionItem}") @Size(max = 64, message = "{mes.valid.inspectionItemCannotExceedCharacters}") String name,
            @NotNull(message = "{mes.valid.selectJudgmentMethod}") ItemMethod method,
            @Size(max = 255) String standard,
            BigDecimal target,
            BigDecimal lowerLimit,
            BigDecimal upperLimit,
            @Size(max = 16) String unit,
            Boolean critical) {
    }

    /**
     * 手工登记不合格（生产发现、客诉等）
     *
     * @param woNo 生产工单号：返工时按工单产品生成返工工单
     */
    public record Ncr(
            @NotNull(message = "{mes.valid.selectSource}") NcrSource source,
            @NotNull(message = "{mes.valid.selectMaterial}") Long materialId,
            @Size(max = 64) String batchNo,
            @NotNull(message = "{mes.valid.enterQuantity}") @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4) BigDecimal qty,
            Long supplierId,
            @Size(max = 32) String woNo,
            @Size(max = 32) String defectType,
            @NotBlank(message = "{mes.valid.enterNonconformanceDescription}") @Size(max = 500, message = "{mes.valid.descriptionCannotExceedCharacters3}") String description) {
    }

    /**
     * 评审处置：提交后按处置执行（启用审批时审批通过后执行）
     *
     * @param reworkWarehouseId 返工工单的完工入库仓库（返工时必填）
     */
    public record NcrReview(
            @NotNull(message = "{mes.valid.selectDisposition}") Disposition disposition,
            @NotBlank(message = "{mes.valid.selectResponsibility}") String responsibility,
            @Size(max = 500, message = "{mes.valid.reviewOpinionCannotExceedCharacters}") String reviewOpinion,
            Long reworkWarehouseId) {
    }

    public record Capa(
            @NotNull(message = "{mes.valid.selectSource}") CapaSource source,
            Long ncrId,
            @NotBlank(message = "{mes.valid.enterProblemTitle}") @Size(max = 128, message = "{mes.valid.titleCannotExceedCharacters}") String title,
            @Size(max = 1000, message = "{mes.valid.descriptionCannotExceedCharacters4}") String description,
            @NotNull(message = "{mes.valid.selectOwner}") Long ownerId,
            @NotNull(message = "{mes.valid.selectDueDate2}") LocalDate dueDate) {
    }

    public record CapaStep(
            @Size(max = 2000, message = "{mes.valid.contentCannotExceedCharacters2}") String content,
            Long ownerId,
            Boolean done) {
    }

    /** 效果验证：有效则关闭，无效退回处理中 */
    public record CapaVerify(
            @NotNull(message = "{mes.valid.selectVerificationConclusion}") Boolean effective,
            @NotBlank(message = "{mes.valid.enterVerificationNotes}") @Size(max = 500, message = "{mes.valid.verificationNotesCannotExceedCharacters}") String verifyResult) {
    }

    public record Claim(
            @NotNull(message = "{mes.valid.selectSupplier}") Long supplierId,
            Long ncrId,
            @NotNull(message = "{mes.valid.selectClaimDate}") LocalDate claimDate,
            @NotNull(message = "{mes.valid.enterClaimAmount}") @DecimalMin(value = "0", inclusive = false, message = "{mes.valid.claimAmountMustGreater}")
            @Digits(integer = 16, fraction = 2, message = "{mes.valid.amountAllowsMostDecimalPlaces}") BigDecimal amount,
            @NotBlank(message = "{mes.valid.enterClaimReason}") @Size(max = 500, message = "{mes.valid.claimReasonCannotExceedCharacters}") String reason) {
    }
}
