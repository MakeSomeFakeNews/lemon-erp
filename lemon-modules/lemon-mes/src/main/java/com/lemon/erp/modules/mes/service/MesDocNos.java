package com.lemon.erp.modules.mes.service;

import com.lemon.erp.framework.code.DocNoCatalog;
import com.lemon.erp.modules.mes.model.WoDocKind;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.lemon.erp.framework.code.DocNoCatalog.Type.doc;

/**
 * 生产制造模块的单据编号（编码规则页面可配置），规则标识与 docNoService.next 的参数一致
 */
@Configuration
public class MesDocNos {

    /** 生产计划 */
    public static final String PLAN = "JH";
    /** MRP 运算 */
    public static final String MRP_RUN = "MR";
    /** 生产工单 */
    public static final String WORK_ORDER = "SC";
    /** 返工工单 */
    public static final String REWORK_ORDER = "FG";
    /** 补料申请 */
    public static final String SUPPLEMENT = "BS";
    /** 设备维护 */
    public static final String MAINTENANCE = "WX";

    @Bean
    @Order(3)
    public DocNoCatalog mesDocNoCatalog() {
        List<DocNoCatalog.Type> types = new ArrayList<>();
        types.add(doc(PLAN, "mes.label.productionPlan"));
        types.add(doc(MRP_RUN, "mes.label.mrpRun"));
        types.add(doc(WORK_ORDER, "mes.label.workOrder"));
        types.add(doc(REWORK_ORDER, "mes.label.reworkOrder"));
        Arrays.stream(WoDocKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getDocName())));
        types.add(doc(SUPPLEMENT, "mes.label.supplementaryIssueRequest"));
        types.add(doc(MAINTENANCE, "mes.label.equipmentMaintenance"));
        types.add(doc(MesEcnService.DOC_NO, "mes.label.ecn"));
        types.add(doc(MesTechDocService.DOC_NO, "mes.label.technicalDocument"));
        return new DocNoCatalog("mes.label.manufacturing", types);
    }

    @Bean
    @Order(4)
    public DocNoCatalog qmsDocNoCatalog() {
        return new DocNoCatalog("mes.label.qualityManagement", List.of(
                doc(MesInspectionService.DOC_NO, "mes.label.qualityInspection"),
                doc(QmsNcrService.DOC_NO, "mes.label.nonconformanceReport"),
                doc(QmsCapaService.DOC_NO, "mes.label.capaD"),
                doc(QmsClaimService.DOC_NO, "mes.label.supplierQualityClaim")));
    }
}
