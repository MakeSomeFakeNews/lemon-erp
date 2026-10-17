package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.period.BizPeriodGuard;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocMapper;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.lemon.erp.modules.wms.model.StockBizType.TRANSFER;

/**
 * 出入库单冲销：为已完成的手工出入库单、调拨单生成红字单据，库存按原单反向变动（入库冲销要求库存仍在原库位）。
 * 在调用方（{@link WmsStockDocService#reverse}）的事务中执行
 */
@Component
@RequiredArgsConstructor
public class StockDocReverser {

    private final WmsStockDocMapper docMapper;
    private final WmsStockDocItemMapper itemMapper;
    private final DocNoService docNoService;
    private final StockService stockService;
    private final BizPeriodGuard periodGuard;

    /**
     * @param doc 已按数据权限读取的原单
     * @return 红字单据 ID
     */
    public Long reverse(StockBizType kind, WmsStockDoc doc) {
        StockDocRules.checkReversible(doc);
        Long id = doc.getId();
        LocalDate bizDate = LocalDate.now();
        if (kind == TRANSFER) {
            periodGuard.checkOpen(bizDate, "wms.label.reversal");
        } else {
            periodGuard.checkStockOpen(bizDate, "wms.label.reversal");
        }
        if (docMapper.update(Wrappers.<WmsStockDoc>lambdaUpdate().set(WmsStockDoc::getReversed, 1)
                .eq(WmsStockDoc::getId, id).eq(WmsStockDoc::getReversed, 0)) == 0) {
            throw new BizException("wms.error.documentHasReversed");
        }
        List<WmsStockDocItem> items = itemMapper.selectList(Wrappers.<WmsStockDocItem>lambdaQuery()
                .eq(WmsStockDocItem::getDocId, id).orderByAsc(WmsStockDocItem::getId));
        LoginUser user = SecurityUtils.getLoginUser();
        WmsStockDoc red = new WmsStockDoc();
        red.setDocNo(docNoService.next(kind));
        red.setKind(kind);
        red.setBizType(doc.getBizType());
        red.setIoCategory(doc.getIoCategory());
        red.setWarehouseId(doc.getWarehouseId());
        red.setToWarehouseId(doc.getToWarehouseId());
        red.setBizDate(bizDate);
        red.setRemark("冲销 " + doc.getDocNo()); // i18n-ignore 存入单据备注
        red.setInTransit(0);
        red.setRedOf(id);
        red.setReversed(0);
        red.setTotalQty(doc.getTotalQty().negate());
        red.setStatus(DocStatus.DONE);
        red.setDeptId(user.deptId());
        red.setConfirmBy(user.userId());
        red.setConfirmTime(LocalDateTime.now());
        docMapper.insert(red);
        List<WmsStockDocItem> redItems = items.stream().map(StockDocReverser::redItem).toList();
        redItems.forEach(i -> i.setDocId(red.getId()));
        itemMapper.insertBatch(redItems);
        stockService.apply(kind, red.getId(), red.getDocNo(), StockDocRules.reverseChanges(kind, doc, items));
        return red.getId();
    }

    /** 红字明细：数量取反，其余与原明细相同 */
    private static WmsStockDocItem redItem(WmsStockDocItem i) {
        WmsStockDocItem r = new WmsStockDocItem();
        r.setMaterialId(i.getMaterialId());
        r.setLocationId(i.getLocationId());
        r.setToLocationId(i.getToLocationId());
        r.setBatchNo(i.getBatchNo());
        r.setProductionDate(i.getProductionDate());
        r.setExpiryDate(i.getExpiryDate());
        r.setQty(i.getQty().negate());
        r.setSerialNos(i.getSerialNos());
        r.setRemark(i.getRemark());
        return r;
    }
}
