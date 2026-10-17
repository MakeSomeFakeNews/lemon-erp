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

    @Bean
    @Order(3)
    public DocNoCatalog mesDocNoCatalog() {
        List<DocNoCatalog.Type> types = new ArrayList<>();
        types.add(doc("JH", "生产计划"));
        types.add(doc("MR", "MRP 运算"));
        types.add(doc("SC", "生产工单"));
        types.add(doc("FG", "返工工单"));
        Arrays.stream(WoDocKind.values()).forEach(k -> types.add(doc(k.getPrefix(), k.getLabel() + "单"))); // i18n-ignore
        types.add(doc("BS", "补料申请"));
        types.add(doc("WX", "设备维修"));
        types.add(doc(MesEcnService.DOC_NO, "工程变更"));
        types.add(doc(MesTechDocService.DOC_NO, "技术文档"));
        return new DocNoCatalog("生产制造", types);
    }

    @Bean
    @Order(4)
    public DocNoCatalog qmsDocNoCatalog() {
        return new DocNoCatalog("质量管理", List.of(
                doc(MesInspectionService.DOC_NO, "质量检验"),
                doc(QmsNcrService.DOC_NO, "不合格品处理"),
                doc(QmsCapaService.DOC_NO, "纠正预防（8D）"),
                doc(QmsClaimService.DOC_NO, "供应商质量索赔")));
    }
}
