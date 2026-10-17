package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.excel.ImportRow;
import com.lemon.erp.modules.wms.dto.OpeningStockRow;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.mapper.WmsLocationMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsWarehouseMapper;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.WmsConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 期初库存导入：系统启用时按 Excel 导入各仓库的结存（仓库、库位、物料、批次、序列号、数量），
 * 按仓库生成已确认的「期初入库」单（每张最多 500 行），业务日期为启用期间的前一天。任何一行有错误时整批不导入
 */
@Service
@RequiredArgsConstructor
public class WmsOpeningStockService {

    private static final int DOC_ROWS = 500;

    private final WmsStockDocService docService;
    private final WmsWarehouseMapper warehouseMapper;
    private final WmsLocationMapper locationMapper;
    private final WmsMaterialMapper materialMapper;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    /**
     * @param bizDate 期初入库单的业务日期
     * @return 导入结果；成功时 imported 为行数
     */
    public ImportResult importRows(ImportRequest<OpeningStockRow> req, LocalDate bizDate) {
        List<ImportRow<OpeningStockRow>> rows = req.rows().stream().filter(r -> r != null && r.data() != null).toList();
        List<ImportResult.RowError> errors = new ArrayList<>();
        List<WmsWarehouse> warehouses = warehouseMapper.selectList(null);
        Set<String> codes = rows.stream().map(r -> r.data().materialCode()).filter(StringUtils::hasText).map(String::trim)
                .collect(Collectors.toSet());
        Map<String, WmsMaterial> materials = codes.isEmpty() ? Map.of()
                : materialMapper.selectList(Wrappers.<WmsMaterial>lambdaQuery().in(WmsMaterial::getCode, codes)).stream()
                .collect(Collectors.toMap(WmsMaterial::getCode, Function.identity()));
        Map<Long, Map<String, WmsLocation>> locations = new LinkedHashMap<>();
        // 仓库 → 明细（保持 Excel 顺序），记录每行的行号便于提示
        Map<Long, List<ImportRow<StockDocItemRequest>>> byWarehouse = new LinkedHashMap<>();
        for (ImportRow<OpeningStockRow> row : rows) {
            OpeningStockRow d = row.data();
            String error = validate(d);
            if (error == null) {
                WmsWarehouse wh = warehouses.stream().filter(w -> w.getCode().equals(d.warehouse().trim()) || w.getName().equals(d.warehouse().trim()))
                        .findFirst().orElse(null);
                WmsMaterial m = materials.get(d.materialCode().trim());
                if (wh == null) {
                    error = Messages.get("wms.text.warehouseDoesNotExist", d.warehouse());
                } else if (m == null) {
                    error = Messages.get("wms.text.materialDoesNotExist", d.materialCode());
                } else {
                    Long locationId = null;
                    if (StringUtils.hasText(d.location())) {
                        WmsLocation loc = locations.computeIfAbsent(wh.getId(), id -> locationMapper.selectList(
                                        Wrappers.<WmsLocation>lambdaQuery().eq(WmsLocation::getWarehouseId, id)).stream()
                                .collect(Collectors.toMap(WmsLocation::getCode, Function.identity(), (a, b) -> a))).get(d.location().trim());
                        if (loc == null) {
                            error = Messages.get("wms.text.warehouseHasNoLocation", wh.getName(), d.location());
                        } else {
                            locationId = loc.getId();
                        }
                    }
                    if (error == null) {
                        byWarehouse.computeIfAbsent(wh.getId(), k -> new ArrayList<>()).add(new ImportRow<>(row.rowNum(),
                                new StockDocItemRequest(m.getId(), locationId, null, d.batchNo(), d.productionDate(), d.expiryDate(),
                                        d.qty(), d.serialNos(), null)));
                    }
                }
            }
            if (error != null) {
                errors.add(new ImportResult.RowError(row.rowNum(), error));
            }
        }
        if (!errors.isEmpty()) {
            return new ImportResult(rows.size(), 0, errors);
        }
        transactionTemplate.executeWithoutResult(status -> {
            // 出错后不再继续（事务已标记回滚）
            byWarehouse.forEach((warehouseId, items) -> {
                for (int i = 0; i < items.size() && errors.isEmpty(); i += DOC_ROWS) {
                    List<ImportRow<StockDocItemRequest>> chunk = items.subList(i, Math.min(i + DOC_ROWS, items.size()));
                    try {
                        docService.createConfirmed(StockBizType.IN, new StockDocRequest(WmsConstants.OPENING, warehouseId, null, bizDate,
                                "期初库存导入", chunk.stream().map(ImportRow::data).toList()), null); // i18n-ignore 存入单据备注
                    } catch (BizException e) {
                        String range = chunk.get(0).rowNum() + "-" + chunk.get(chunk.size() - 1).rowNum();
                        errors.add(new ImportResult.RowError(chunk.get(0).rowNum(), Messages.get("wms.text.line", range, e.getLocalizedMessage())));
                    }
                }
            });
            if (!errors.isEmpty()) {
                status.setRollbackOnly();
            }
        });
        return new ImportResult(rows.size(), errors.isEmpty() ? rows.size() : 0, errors);
    }

    private String validate(OpeningStockRow data) {
        var violations = validator.validate(data);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream().sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage).distinct().collect(Collectors.joining(Messages.get("wms.text.separator")));
    }
}
