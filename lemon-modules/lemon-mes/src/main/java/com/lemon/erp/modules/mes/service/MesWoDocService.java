package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.WoDocItemRequest;
import com.lemon.erp.modules.mes.dto.WoDocRequest;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesWoDoc;
import com.lemon.erp.modules.mes.entity.MesWoDocItem;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesInspectionMapper;
import com.lemon.erp.modules.mes.mapper.MesWoDocItemMapper;
import com.lemon.erp.modules.mes.mapper.MesWoDocMapper;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.WoDocKind;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.WarehouseType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.wms.model.WmsConstants.NO_BATCH;
import static com.lemon.erp.modules.wms.model.WmsConstants.batchOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.joinSerials;
import static com.lemon.erp.modules.wms.model.WmsConstants.locationOrNone;
import static com.lemon.erp.modules.wms.model.WmsConstants.splitSerials;

/**
 * 工单领料 / 退料 / 完工入库 / 补料：保存即生成并确认仓库单据（生产领料出库、生产退料入库、生产入库），同一事务回写工单。
 * <ul>
 *   <li>领料、退料：物料必须是工单用料，退料不超过已领数量；
 *       累计领料不超过 需求 × (1 + 物料的允许超领比例) + 已补料，超出部分走补料申请（{@link MesSupplementService}）</li>
 *   <li>补料：补料申请审批通过后发料，不受领料上限限制，同时计入已领和已补料</li>
 *   <li>完工入库：物料为工单产品，累计不超过工单数量，满数量后工单自动完工</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MesWoDocService {

    private final MesWoDocMapper docMapper;
    private final MesWoDocItemMapper itemMapper;
    private final MesWoMaterialMapper woMaterialMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesWorkOrderService workOrderService;
    private final MesMaterialService materialPlanService;
    private final WmsStockDocService stockDocService;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final MesWoOpMapper woOpMapper;
    private final MesInspectionMapper inspectionMapper;
    private final com.lemon.erp.modules.wms.service.WmsWarehouseService warehouseService;

    public PageResult<MesWoDoc> page(long current, long size, Long woId, WoDocKind kind) {
        if (woId != null) {
            workOrderService.getAccessible(woId);
        }
        Page<MesWoDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, woId, kind);
        return PageResult.of(page);
    }

    public MesWoDoc detail(Long id) {
        MesWoDoc doc = docMapper.selectDetail(id);
        if (doc == null) {
            throw new BizException("mes.error.documentDoesNotExist");
        }
        workOrderService.getAccessible(doc.getWoId());
        doc.setItems(itemMapper.selectByDocId(id));
        return doc;
    }

    /** 领料 / 退料 / 完工入库，返回新单据 ID */
    @Transactional
    public Long create(Long woId, WoDocKind kind, WoDocRequest req) {
        if (kind == WoDocKind.SUPPLEMENT) {
            throw new BizException("mes.error.submitSupplementaryIssueRequestSupplementaryIssues");
        }
        return save(woId, kind, req);
    }

    /** 补料申请发料：不受领料上限限制，计入已补料；由 {@link MesSupplementService} 校验申请数量 */
    @Transactional
    public MesWoDoc issueSupplement(Long woId, WoDocRequest req) {
        return docMapper.selectById(save(woId, WoDocKind.SUPPLEMENT, req));
    }

    private Long save(Long woId, WoDocKind kind, WoDocRequest req) {
        MesWorkOrder wo = workOrderService.getExecutable(woId);
        Map<Long, WmsMaterial> materials = materialService.mapByIds(req.items().stream().map(WoDocItemRequest::materialId).toList());
        BigDecimal total = Decimals.sum(req.items(), WoDocItemRequest::qty);

        if (kind == WoDocKind.FINISH) {
            if (req.items().stream().anyMatch(i -> !i.materialId().equals(wo.getMaterialId()))) {
                throw new BizException("mes.error.completionReceiptMaterialMustWorkOrder");
            }
            if (workOrderMapper.addFinished(woId, total) == 0) {
                BigDecimal remaining = wo.getQty().subtract(workOrderMapper.selectById(woId).getFinishedQty());
                throw new BizException("mes.error.completedQuantityExceedsUnfinishedQuantity", remaining.stripTrailingZeros().toPlainString());
            }
            checkFinishLimit(wo, total);
        } else if (kind == WoDocKind.SCRAP) {
            checkScrap(wo, req, total);
        } else {
            Map<Long, MesWoMaterial> woMaterials = woMaterialMapper.selectByWo(woId).stream()
                    .collect(Collectors.toMap(MesWoMaterial::getMaterialId, Function.identity()));
            Map<Long, MesMaterialPlan> plans = kind == WoDocKind.ISSUE ? materialPlanService.plans(materials.keySet()) : Map.of();
            // 同一物料拆成多行（多个库位 / 批次）时按合计校验上限
            Map<Long, BigDecimal> qtyByMaterial = new LinkedHashMap<>();
            req.items().forEach(i -> qtyByMaterial.merge(i.materialId(), i.qty(), BigDecimal::add));
            for (Map.Entry<Long, BigDecimal> e : qtyByMaterial.entrySet()) {
                MesWoMaterial wm = woMaterials.get(e.getKey());
                WmsMaterial m = materials.get(e.getKey());
                String name = m == null ? String.valueOf(e.getKey()) : m.getCode() + " " + m.getName();
                if (wm == null) {
                    throw new BizException("mes.error.materialNotWorkOrderMaterial", name);
                }
                switch (kind) {
                    case ISSUE -> {
                        BigDecimal rate = MesMaterialService.overIssueRate(plans.get(e.getKey()));
                        if (woMaterialMapper.addIssued(wm.getId(), e.getValue(), rate) == 0) {
                            throw new BizException(overIssueMessage(name, wm, rate));
                        }
                    }
                    case RETURN -> {
                        if (woMaterialMapper.addIssued(wm.getId(), e.getValue().negate(), null) == 0) {
                            throw new BizException("mes.error.returnQuantityMaterialExceedsIssuedQuantity", name);
                        }
                    }
                    default -> woMaterialMapper.addSupplement(wm.getId(), e.getValue());
                }
            }
        }

        String docNo = docNoService.next(kind.getPrefix());
        List<MesWoDocItem> items = req.items().stream().map(r -> toItem(kind, r, materials.get(r.materialId()))).toList();
        StockDocRequest stockReq = new StockDocRequest(kind.getStockBizType(), req.warehouseId(), null, req.bizDate(),
                truncate(req.remark() == null ? "工单 " + wo.getWoNo() : "工单 " + wo.getWoNo() + " " + req.remark()), // i18n-ignore
                items.stream().map(i -> new StockDocItemRequest(i.getMaterialId(), i.getLocationId(), null, i.getBatchNo(),
                        i.getProductionDate(), i.getExpiryDate(), i.getQty(), i.getSerialNos(), null)).toList());
        WmsStockDoc stockDoc = stockDocService.createConfirmed(kind.getStockKind(), stockReq, docNo);

        MesWoDoc doc = new MesWoDoc();
        doc.setDocNo(docNo);
        doc.setKind(kind);
        doc.setWoId(woId);
        doc.setWarehouseId(req.warehouseId());
        doc.setBizDate(req.bizDate());
        doc.setTotalQty(total);
        doc.setWmsDocId(stockDoc.getId());
        doc.setWmsDocNo(stockDoc.getDocNo());
        doc.setRemark(req.remark());
        doc.setCreateBy(SecurityUtils.getUserId());
        docMapper.insert(doc);
        items.forEach(i -> i.setDocId(doc.getId()));
        itemMapper.insertBatch(items);

        workOrderService.markStarted(woId);
        if (kind == WoDocKind.FINISH) {
            workOrderService.markCompleted(woId);
        }
        return doc.getId();
    }

    /**
     * 完工入库累计数量的上限：使用了报工的工单不超过末道工序的累计合格数；产品设置了「完工需成品检验」时不超过已确认的成品检验合格数
     */
    private void checkFinishLimit(MesWorkOrder wo, BigDecimal qty) {
        BigDecimal after = wo.getFinishedQty().add(qty);
        List<MesWoOp> ops = woOpMapper.selectByWo(wo.getId());
        if (!ops.isEmpty() && ops.stream().anyMatch(op -> op.getGoodQty().signum() > 0 || op.getScrapQty().signum() > 0)) {
            MesWoOp last = ops.stream().max(Comparator.comparing(MesWoOp::getSeq)).orElseThrow();
            if (after.compareTo(last.getGoodQty()) > 0) {
                throw new BizException("mes.error.completedQuantityExceedsCumulativeGoodQuantity", last.getName(), plain(last.getGoodQty()), plain(wo.getFinishedQty()));
            }
        }
        MesMaterialPlan plan = materialPlanService.plans(Set.of(wo.getMaterialId())).get(wo.getMaterialId());
        if (plan != null && Integer.valueOf(1).equals(plan.getFqcRequired())) {
            BigDecimal passed = Decimals.nz(inspectionMapper.sumFqcPassed(wo.getId()));
            if (after.compareTo(passed) > 0) {
                throw new BizException("mes.error.productRequiresFinalInspectionBeforeCompletion", plain(passed), plain(wo.getFinishedQty()));
            }
        }
    }

    /** 报废入库：工单产品入不良品仓，累计不超过各工序报工的报废数量合计 */
    private void checkScrap(MesWorkOrder wo, WoDocRequest req, BigDecimal qty) {
        if (req.items().stream().anyMatch(i -> !i.materialId().equals(wo.getMaterialId()))) {
            throw new BizException("mes.error.scrapReceiptMaterialMustWorkOrder");
        }
        if (warehouseService.get(req.warehouseId()).getType() != WarehouseType.DEFECT) {
            throw new BizException("mes.error.selectDefectiveGoodsWarehouseScrapReceipts");
        }
        BigDecimal scrapped = Decimals.sum(woOpMapper.selectByWo(wo.getId()), MesWoOp::getScrapQty);
        BigDecimal received = Decimals.sum(docMapper.selectList(Wrappers.<MesWoDoc>lambdaQuery().eq(MesWoDoc::getWoId, wo.getId())
                .eq(MesWoDoc::getKind, WoDocKind.SCRAP)), MesWoDoc::getTotalQty);
        if (received.add(qty).compareTo(scrapped) > 0) {
            throw new BizException("mes.error.scrapReceiptQuantityExceedsReportedScrap", plain(scrapped), plain(received));
        }
    }

    /** 领料上限 = 需求 × (1 + 允许超领比例) + 已补料 */
    private static String overIssueMessage(String name, MesWoMaterial wm, BigDecimal rate) {
        BigDecimal limit = wm.getRequiredQty().multiply(Decimals.HUNDRED.add(rate))
                .divide(Decimals.HUNDRED, 4, RoundingMode.HALF_UP).add(wm.getSupplementQty());
        BigDecimal left = limit.subtract(wm.getIssuedQty()).max(BigDecimal.ZERO);
        return Messages.get("mes.text.materialExceedsIssueLimitRequired", name, plain(wm.getRequiredQty()))
                + (rate.signum() > 0 ? Messages.get("mes.text.overIssueAllowance", plain(rate)) : "")
                + (wm.getSupplementQty().signum() > 0 ? Messages.get("mes.text.supplemented", plain(wm.getSupplementQty())) : "")
                + Messages.get("mes.text.issuedMostMoreIssuedSubmitSupplementary", plain(wm.getIssuedQty()), plain(left));
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static String truncate(String text) {
        return text.length() > 255 ? text.substring(0, 255) : text;
    }

    /** 按物料的管理方式规范批次、序列号；生产日期、有效期仅完工入库 */
    private static MesWoDocItem toItem(WoDocKind kind, WoDocItemRequest r, WmsMaterial material) {
        MesWoDocItem item = new MesWoDocItem();
        item.setMaterialId(r.materialId());
        item.setLocationId(locationOrNone(r.locationId()));
        boolean batch = material != null && material.isBatchManaged();
        item.setBatchNo(batch ? batchOrNone(r.batchNo()) : NO_BATCH);
        if ((kind == WoDocKind.FINISH || kind == WoDocKind.SCRAP) && batch) {
            item.setProductionDate(r.productionDate());
            item.setExpiryDate(r.expiryDate());
        }
        item.setQty(r.qty());
        item.setSerialNos(material != null && material.isSerialManaged() ? joinSerials(splitSerials(r.serialNos())) : null);
        return item;
    }
}
