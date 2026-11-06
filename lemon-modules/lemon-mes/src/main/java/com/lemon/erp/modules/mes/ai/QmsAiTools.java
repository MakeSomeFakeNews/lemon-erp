package com.lemon.erp.modules.mes.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.mes.dto.QualitySummary;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.model.Disposition;
import com.lemon.erp.modules.mes.model.NcrSource;
import com.lemon.erp.modules.mes.model.NcrStatus;
import com.lemon.erp.modules.mes.model.QualitySummaryBy;
import com.lemon.erp.modules.mes.service.MesInspectionService;
import com.lemon.erp.modules.mes.service.QmsNcrService;
import com.lemon.erp.modules.wms.ai.AiQuerySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.wms.ai.AiQuerySupport.END_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.LIMIT;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.START_DATE;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.limit;
import static com.lemon.erp.modules.wms.ai.AiQuerySupport.plain;

/**
 * 质量管理的 AI 问数工具：检验合格率（按检验类型 / 供应商）、不合格品（NCR）。与对应页面相同的数据
 */
public final class QmsAiTools {

    /** 不合格描述最多发给模型的字符数 */
    private static final int DESCRIPTION_MAX = 100;

    private QmsAiTools() {
    }

    // ---------------------------------------------------------------- 检验合格率

    /**
     * 检验汇总：groupKey 为检验类型（IQC 来料、IPQC 过程、FQC 成品）或供应商编码；passRate 批次合格率（%，让步接收不计入合格），
     * defectRate 抽样不良率（%）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PassRateRow(String groupKey, String groupName, Long totalCount, Long passCount, Long failCount,
                              Long concessionCount, BigDecimal passRate, BigDecimal sampleQty, BigDecimal defectQty,
                              BigDecimal defectRate) {
    }

    @Component
    @RequiredArgsConstructor
    public static class PassRateTool implements AiTool {

        private final MesInspectionService inspectionService;

        public String name() {
            return "qms_inspection_pass_rate";
        }

        public String labelKey() {
            return "mes.ai.tool.passRate";
        }

        public String descriptionKey() {
            return "mes.ai.tool.passRate.desc";
        }

        public String permission() {
            return "mes:qc:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("groupBy", AiSchema.enumOf("mes.ai.param.qcGroupBy",
                            Arrays.stream(QualitySummaryBy.values()).map(Enum::name).toArray(String[]::new)), false)
                    .prop(START_DATE, AiQuerySupport.startDate(), false)
                    .prop(END_DATE, AiQuerySupport.endDate(), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            String by = args.getString("groupBy");
            AiQuerySupport.Range range = AiQuerySupport.monthToDate(args);
            List<QualitySummary> list = inspectionService.summary(by == null ? QualitySummaryBy.TYPE : QualitySummaryBy.valueOf(by),
                    range.begin(), range.end());
            return AiToolResult.rows(list.stream().limit(limit(args)).map(q -> new PassRateRow(q.getGroupKey(), q.getGroupName(),
                    q.getTotalCount(), q.getPassCount(), q.getFailCount(), q.getConcessionCount(), q.getPassRate(),
                    plain(q.getSampleQty()), plain(q.getDefectQty()), q.getDefectRate())).toList(), (long) list.size());
        }
    }

    // ---------------------------------------------------------------- 不合格品

    /** 不合格品报告：defectType 为不良类型（业务字典的取值），disposition 为处置方式 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NcrRow(String ncrNo, @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime createTime, NcrSource source,
                         String sourceNo, String materialCode, String materialName, String batchNo, BigDecimal qty, String unit,
                         String supplierName, String woNo, String defectType, String description, Disposition disposition,
                         NcrStatus status) {
    }

    @Component
    @RequiredArgsConstructor
    public static class NcrTool implements AiTool {

        private final QmsNcrService ncrService;

        public String name() {
            return "qms_nonconformities";
        }

        public String labelKey() {
            return "mes.ai.tool.ncr";
        }

        public String descriptionKey() {
            return "mes.ai.tool.ncr.desc";
        }

        public String permission() {
            return "qms:ncr:list";
        }

        public Map<String, Object> parameters() {
            return AiSchema.object()
                    .prop("status", AiSchema.enumOf("mes.ai.param.ncrStatus",
                            Arrays.stream(NcrStatus.values()).map(Enum::name).toArray(String[]::new)), false)
                    .prop("keyword", AiSchema.string("mes.ai.param.ncrKeyword").maxLength(64), false)
                    .prop(LIMIT, AiQuerySupport.limit(), false)
                    .build();
        }

        public Object execute(AiToolArgs args) {
            String status = args.getString("status");
            PageResult<QmsNcr> page = ncrService.page(1, limit(args), status == null ? null : NcrStatus.valueOf(status),
                    args.getString("keyword"));
            return AiToolResult.rows(page.records().stream().map(n -> new NcrRow(n.getNcrNo(), n.getCreateTime(), n.getSource(),
                    n.getSourceNo(), n.getMaterialCode(), n.getMaterialName(), n.getBatchNo() == null || n.getBatchNo().isEmpty() ? null : n.getBatchNo(),
                    plain(n.getQty()), n.getUnit(), n.getSupplierName(), n.getWoNo(), n.getDefectType(), shorten(n.getDescription()),
                    n.getDisposition(), n.getStatus())).toList(), page.total());
        }
    }

    static String shorten(String s) {
        if (s == null || s.length() <= DESCRIPTION_MAX) {
            return s;
        }
        return s.substring(0, DESCRIPTION_MAX) + "...";
    }
}
