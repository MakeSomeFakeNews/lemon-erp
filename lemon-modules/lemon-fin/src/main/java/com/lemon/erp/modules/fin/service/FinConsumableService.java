package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.fin.dto.ConsumableRequest;
import com.lemon.erp.modules.fin.entity.FinConsumableUse;
import com.lemon.erp.modules.fin.entity.FinConsumableUseItem;
import com.lemon.erp.modules.fin.mapper.FinConsumableUseMapper;
import com.lemon.erp.modules.fin.model.ConsumableStatus;
import com.lemon.erp.modules.fin.model.FinConstants;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 低值易耗品领用：物料分类为「低值易耗品」（存货科目 周转材料），部门领用时生成「部门领用」出库单；
 * 月末成本核算按实际单价一次摊销：借 领用单的费用科目（按领用部门），贷 周转材料
 */
@Service
@RequiredArgsConstructor
public class FinConsumableService {

    /** 领用单号的编码规则标识 */
    public static final String DOC_NO = "YH";

    private final FinConsumableUseMapper useMapper;
    private final FinAccountService accountService;
    private final WmsMaterialService materialService;
    private final WmsStockDocService stockDocService;
    private final DocNoService docNoService;

    public PageResult<FinConsumableUse> page(long current, long size, ConsumableStatus status, Long deptId, String keyword) {
        Page<FinConsumableUse> page = Page.of(current, size);
        useMapper.selectPageList(page, status, deptId, keyword);
        return PageResult.of(page);
    }

    public FinConsumableUse detail(Long id) {
        FinConsumableUse use = id == null ? null : useMapper.selectDetail(id);
        if (use == null) {
            throw new BizException("fin.error.requisitionNotFound");
        }
        use.setItems(useMapper.selectItems(id));
        return use;
    }

    /** 领用台账：按部门、物料汇总（参考金额） */
    public List<Map<String, Object>> ledger(LocalDate begin, LocalDate end, Long deptId) {
        return useMapper.selectLedger(begin, end, deptId);
    }

    @Transactional
    public Long create(ConsumableRequest req) {
        validate(req);
        FinConsumableUse use = new FinConsumableUse();
        use.setUseNo(docNoService.next(DOC_NO));
        apply(use, req);
        use.setStatus(ConsumableStatus.DRAFT);
        useMapper.insert(use);
        saveItems(use.getId(), req);
        return use.getId();
    }

    @Transactional
    public void update(Long id, ConsumableRequest req) {
        FinConsumableUse use = requireDraft(id, "fin.error.requisitionHasIssuedCannotModified");
        validate(req);
        apply(use, req);
        useMapper.updateById(use);
        useMapper.deleteItems(id);
        saveItems(id, req);
    }

    @Transactional
    public void delete(Long id) {
        requireDraft(id, "fin.error.requisitionHasIssuedCannotDeleted");
        useMapper.deleteItems(id);
        useMapper.deleteById(id);
    }

    /** 出库：生成并确认「部门领用」出库单 */
    @Transactional
    public void confirm(Long id) {
        FinConsumableUse use = requireDraft(id, "fin.error.requisitionHasAlreadyIssued");
        List<FinConsumableUseItem> items = useMapper.selectItems(id);
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.CONSUME, use.getWarehouseId(), null,
                use.getUseDate(), "低值易耗品领用 " + use.getUseNo() // i18n-ignore
                + (StringUtils.hasText(use.getRemark()) ? "：" + use.getRemark() : ""),
                items.stream().map(i -> new StockDocItemRequest(i.getMaterialId(), i.getLocationId(), null, i.getBatchNo(), null, null,
                        i.getQty(), null, i.getRemark())).toList()), use.getUseNo());
        StatusTransitions.change(useMapper, FinConsumableUse::getId, id, FinConsumableUse::getStatus,
                ConsumableStatus.DRAFT, ConsumableStatus.CONFIRMED, w -> w.set(FinConsumableUse::getWmsDocId, doc.getId()), "fin.error.requisitionStatusHasChangedRefreshTry");
    }

    private void validate(ConsumableRequest req) {
        accountService.requireLeaves(List.of(req.expenseAccountId()));
        Map<Long, WmsMaterial> materials = materialService.getEnabled(req.items().stream().map(ConsumableRequest.Item::materialId).toList());
        for (WmsMaterial m : materials.values()) {
            if (!FinConstants.LOW_VALUE_CATEGORY.equals(m.getCategory())) {
                throw new BizException("fin.error.materialNotLowValueConsumable", m.getCode(), m.getName());
            }
            if (m.isSerialManaged()) {
                throw new BizException("fin.error.materialUsesSerialNumberManagementManage", m.getCode(), m.getName());
            }
        }
    }

    private static void apply(FinConsumableUse use, ConsumableRequest req) {
        use.setDeptId(req.deptId());
        use.setUserId(req.userId());
        use.setWarehouseId(req.warehouseId());
        use.setUseDate(req.useDate());
        use.setExpenseAccountId(req.expenseAccountId());
        use.setRemark(req.remark());
    }

    private void saveItems(Long useId, ConsumableRequest req) {
        useMapper.insertItems(req.items().stream().map(r -> {
            FinConsumableUseItem item = new FinConsumableUseItem();
            item.setUseId(useId);
            item.setMaterialId(r.materialId());
            item.setLocationId(r.locationId());
            item.setBatchNo(StringUtils.hasText(r.batchNo()) ? r.batchNo().trim() : null);
            item.setQty(r.qty());
            item.setRemark(r.remark());
            return item;
        }).toList());
    }

    /** @param error 已出库时的提示（中文模板） */
    private FinConsumableUse requireDraft(Long id, String error) {
        FinConsumableUse use = id == null ? null : useMapper.selectById(id);
        if (use == null) {
            throw new BizException("fin.error.requisitionNotFound");
        }
        if (use.getStatus() != ConsumableStatus.DRAFT) {
            throw new BizException(error);
        }
        return use;
    }
}
