package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.CapaSource;
import com.lemon.erp.modules.mes.model.Disposition;
import com.lemon.erp.modules.mes.model.InspectionLevel;
import com.lemon.erp.modules.mes.model.ItemMethod;
import com.lemon.erp.modules.mes.model.NcrSource;
import com.lemon.erp.modules.mes.model.QcType;
import com.lemon.erp.modules.mes.model.Responsibility;
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
            @NotBlank(message = "请填写方案名称") @Size(max = 64, message = "方案名称不能超过64个字符") String name,
            @NotNull(message = "请选择检验类型") QcType inspType,
            Long materialId,
            @Size(max = 32) String materialCategory,
            @NotNull(message = "请选择抽样方式") SamplingMode samplingMode,
            @DecimalMin(value = "0", inclusive = false, message = "抽样数量必须大于0") BigDecimal fixedQty,
            @Min(value = 0, message = "允收不良数不能小于0") Integer acceptQty,
            BigDecimal aql,
            InspectionLevel inspectionLevel,
            Integer status,
            @Size(max = 255) String remark,
            @NotEmpty(message = "请添加检验项目") @Size(max = 100, message = "检验项目不能超过100项") List<@Valid @NotNull PlanItem> items) {
    }

    public record PlanItem(
            @NotBlank(message = "请填写检验项目") @Size(max = 64, message = "检验项目不能超过64个字符") String name,
            @NotNull(message = "请选择判定方式") ItemMethod method,
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
            @NotNull(message = "请选择来源") NcrSource source,
            @NotNull(message = "请选择物料") Long materialId,
            @Size(max = 64) String batchNo,
            @NotNull(message = "请填写数量") @DecimalMin(value = "0", inclusive = false, message = "数量必须大于0")
            @Digits(integer = 14, fraction = 4) BigDecimal qty,
            Long supplierId,
            @Size(max = 32) String woNo,
            @Size(max = 32) String defectType,
            @NotBlank(message = "请填写不合格描述") @Size(max = 500, message = "描述不能超过500个字符") String description) {
    }

    /**
     * 评审处置：提交后按处置执行（启用审批时审批通过后执行）
     *
     * @param reworkWarehouseId 返工工单的完工入库仓库（返工时必填）
     */
    public record NcrReview(
            @NotNull(message = "请选择处置方式") Disposition disposition,
            @NotNull(message = "请选择责任方") Responsibility responsibility,
            @Size(max = 500, message = "评审意见不能超过500个字符") String reviewOpinion,
            Long reworkWarehouseId) {
    }

    public record Capa(
            @NotNull(message = "请选择来源") CapaSource source,
            Long ncrId,
            @NotBlank(message = "请填写问题标题") @Size(max = 128, message = "标题不能超过128个字符") String title,
            @Size(max = 1000, message = "描述不能超过1000个字符") String description,
            @NotNull(message = "请选择负责人") Long ownerId,
            @NotNull(message = "请选择要求完成日期") LocalDate dueDate) {
    }

    public record CapaStep(
            @Size(max = 2000, message = "内容不能超过2000个字符") String content,
            Long ownerId,
            Boolean done) {
    }

    /** 效果验证：有效则关闭，无效退回处理中 */
    public record CapaVerify(
            @NotNull(message = "请选择验证结论") Boolean effective,
            @NotBlank(message = "请填写验证说明") @Size(max = 500, message = "验证说明不能超过500个字符") String verifyResult) {
    }

    public record Claim(
            @NotNull(message = "请选择供应商") Long supplierId,
            Long ncrId,
            @NotNull(message = "请选择索赔日期") LocalDate claimDate,
            @NotNull(message = "请填写索赔金额") @DecimalMin(value = "0", inclusive = false, message = "索赔金额必须大于0")
            @Digits(integer = 16, fraction = 2, message = "金额最多2位小数") BigDecimal amount,
            @NotBlank(message = "请填写索赔原因") @Size(max = 500, message = "索赔原因不能超过500个字符") String reason) {
    }
}
