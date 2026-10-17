package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.data.Queries;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.wms.dto.BarcodeMatch;
import com.lemon.erp.modules.wms.entity.WmsCheckItem;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import com.lemon.erp.modules.wms.entity.WmsAssembly;
import com.lemon.erp.modules.wms.entity.WmsAssemblyItem;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.mapper.WmsAssemblyItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsAssemblyMapper;
import com.lemon.erp.modules.wms.entity.WmsVariantValue;
import com.lemon.erp.modules.wms.mapper.WmsVariantValueMapper;
import com.lemon.erp.modules.wms.model.CostMethod;
import com.lemon.erp.modules.wms.entity.WmsMaterialUnit;
import com.lemon.erp.modules.wms.entity.WmsStockDocItem;
import com.lemon.erp.modules.wms.mapper.WmsCheckItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsLocationMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialUnitMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockDocItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsStockFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WmsMaterialService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("code", "物料编码", "name", "物料名称", "spec", "规格型号", "unit", "计量单位", "category", "分类", // i18n-ignore 字段名写入变更记录
            "batchEnabled", "批次管理", "serialEnabled", "序列号管理", "iqcRequired", "来料检验", "safetyStock", "安全库存", "reorderPoint", "再订货点", "reorderQty", "再订货量", "barcode", "条码", "defaultWarehouseId", "默认仓库", "refCost", "参考成本价", "costMethod", "计价方法", "remark", "备注"); // i18n-ignore

    /** 下拉搜索最多返回的条数 */
    private static final int OPTION_LIMIT = 50;

    private final WmsMaterialMapper materialMapper;
    private final WmsVariantValueMapper variantValueMapper;
    private final WmsAssemblyMapper assemblyMapper;
    private final WmsAssemblyItemMapper assemblyItemMapper;
    private final WmsStockFlowMapper flowMapper;
    private final WmsStockDocItemMapper docItemMapper;
    private final WmsCheckItemMapper checkItemMapper;
    private final ReferenceGuard referenceGuard;
    private final ChangeLogger changeLogger;
    private final WmsMaterialUnitMapper unitMapper;
    private final WmsLocationMapper locationMapper;
    private final DocNoService docNoService;

    /** @param keyword 编码或名称，模糊匹配 */
    /** @param templateId 只看某个款式的 SKU */
    public PageResult<WmsMaterial> page(long current, long size, String keyword, String category, Integer status, Long templateId) {
        Page<WmsMaterial> page = materialMapper.selectPage(Page.of(current, size), Wrappers.<WmsMaterial>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(WmsMaterial::getCode, keyword).or().like(WmsMaterial::getName, keyword))
                .eq(StringUtils.hasText(category), WmsMaterial::getCategory, category)
                .eq(status != null, WmsMaterial::getStatus, status)
                .eq(templateId != null, WmsMaterial::getTemplateId, templateId)
                .orderByDesc(WmsMaterial::getId));
        return PageResult.of(page);
    }

    public WmsMaterial get(Long id) {
        WmsMaterial material = id == null ? null : materialMapper.selectById(id);
        if (material == null) {
            throw new BizException("wms.error.materialNotFound");
        }
        return material;
    }

    /**
     * 按 ID 批量加载启用的物料，供其他模块的单据校验；不存在或已停用时报错
     *
     * @return ID → 物料
     */
    public Map<Long, WmsMaterial> getEnabled(Collection<Long> ids) {
        Map<Long, WmsMaterial> map = ids.isEmpty() ? Map.of() : materialMapper.selectByIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(WmsMaterial::getId, Function.identity()));
        for (Long id : ids) {
            WmsMaterial material = map.get(id);
            if (material == null) {
                throw new BizException("wms.error.materialNotFound2", id);
            }
            if (!Integer.valueOf(1).equals(material.getStatus())) {
                throw new BizException("wms.error.materialDisabled", material.getCode(), material.getName());
            }
        }
        return map;
    }

    /** 按 ID 批量加载物料（含停用的），ID → 物料 */
    public Map<Long, WmsMaterial> mapByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return materialMapper.selectByIds(ids.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(WmsMaterial::getId, Function.identity()));
    }

    /**
     * 按编码批量加载物料（含停用的），编码 → 物料，供其他模块的导入功能按编码识别物料
     *
     * @param codes 物料编码（调用方去除空白），不存在的编码不出现在结果中
     */
    public Map<String, WmsMaterial> mapByCodes(Collection<String> codes) {
        if (codes.isEmpty()) {
            return Map.of();
        }
        return materialMapper.selectList(Wrappers.<WmsMaterial>lambdaQuery().in(WmsMaterial::getCode, codes)).stream()
                .collect(Collectors.toMap(WmsMaterial::getCode, Function.identity()));
    }

    /** 启用的物料，按编码或名称搜索，供单据明细选择 */
    public List<WmsMaterial> options(String keyword) {
        return materialMapper.selectPage(Page.of(1, OPTION_LIMIT, false), Wrappers.<WmsMaterial>lambdaQuery()
                .select(WmsMaterial::getId, WmsMaterial::getCode, WmsMaterial::getName, WmsMaterial::getSpec,
                        WmsMaterial::getUnit, WmsMaterial::getBatchEnabled, WmsMaterial::getSerialEnabled,
                        WmsMaterial::getBarcode, WmsMaterial::getDefaultWarehouseId, WmsMaterial::getDefaultLocationId)
                .eq(WmsMaterial::getStatus, 1)
                // 多规格款式不能直接用于单据，选择具体的 SKU
                .eq(WmsMaterial::getIsTemplate, 0)
                .and(StringUtils.hasText(keyword), w -> w.like(WmsMaterial::getCode, keyword).or().like(WmsMaterial::getName, keyword))
                .orderByAsc(WmsMaterial::getCode)).getRecords();
    }

    @Transactional
    public void create(WmsMaterial material) {
        if (!StringUtils.hasText(material.getCode())) {
            material.setCode(docNoService.next(WmsDocNos.MATERIAL));
        }
        checkCodeUnique(material.getCode(), null);
        material.setId(null);
        // 款式、SKU 只能通过「多规格」生成
        material.setIsTemplate(0);
        material.setTemplateId(null);
        material.setVariantText(null);
        normalize(material);
        checkExtras(material, null);
        if (material.getStatus() == null) material.setStatus(1);
        materialMapper.insert(material);
    }

    /** 已发生库存业务的物料，不能修改批次、序列号管理方式，否则已有库存与新规则不一致 */
    @Transactional
    public void update(Long id, WmsMaterial material) {
        Object before = materialMapper.selectById(id);
        WmsMaterial old = get(id);
        checkCodeUnique(material.getCode(), id);
        normalize(material);
        checkExtras(material, id);
        boolean trackingChanged = !Objects.equals(old.getBatchEnabled(), material.getBatchEnabled())
                || !Objects.equals(old.getSerialEnabled(), material.getSerialEnabled());
        if (trackingChanged && flowMapper.existsByMaterialId(id)) {
            throw new BizException("wms.error.materialHasStockTransactionsBatchSerial");
        }
        material.setId(id);
        material.setStatus(null);
        material.setIsTemplate(null);
        material.setTemplateId(null);
        material.setVariantText(null);
        materialMapper.updateById(material);
        // 可为空的字段：updateById 会跳过 null，单独更新以便清空
        materialMapper.update(Wrappers.<WmsMaterial>lambdaUpdate()
                .set(WmsMaterial::getSafetyStock, material.getSafetyStock())
                .set(WmsMaterial::getReorderPoint, material.getReorderPoint())
                .set(WmsMaterial::getReorderQty, material.getReorderQty())
                .set(WmsMaterial::getBarcode, material.getBarcode())
                .set(WmsMaterial::getDefaultWarehouseId, material.getDefaultWarehouseId())
                .set(WmsMaterial::getDefaultLocationId, material.getDefaultLocationId())
                .set(WmsMaterial::getRefCost, material.getRefCost())
                .eq(WmsMaterial::getId, id));
        changeLogger.log("wms_material", id, before, materialMapper.selectById(id), CHANGE_FIELDS);
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        WmsMaterial entity = new WmsMaterial();
        entity.setId(id);
        entity.setStatus(status);
        materialMapper.updateById(entity);
    }

    /** 已发生库存业务或被单据引用的物料不能删除，可以停用 */
    @Transactional
    public void delete(List<Long> ids) {
        for (WmsMaterial material : materialMapper.selectByIds(ids)) {
            Long id = material.getId();
            boolean used = flowMapper.existsByMaterialId(id)
                    || docItemMapper.exists(Wrappers.<WmsStockDocItem>lambdaQuery().eq(WmsStockDocItem::getMaterialId, id))
                    || checkItemMapper.exists(Wrappers.<WmsCheckItem>lambdaQuery().eq(WmsCheckItem::getMaterialId, id))
                    || assemblyMapper.exists(Wrappers.<WmsAssembly>lambdaQuery().eq(WmsAssembly::getMaterialId, id))
                    || assemblyItemMapper.exists(Wrappers.<WmsAssemblyItem>lambdaQuery().eq(WmsAssemblyItem::getMaterialId, id));
            if (used) {
                throw new BizException("wms.error.materialUsedDocumentsCannotDeletedDisable", material.getName());
            }
            if (material.isTemplateMaterial() && materialMapper.exists(Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getTemplateId, id))) {
                throw new BizException("wms.error.styleStillHasSkusDeleteThem", material.getName());
            }
            referenceGuard.checkDeletable(RefType.MATERIAL, id, Messages.get("wms.text.material", material.getCode(), material.getName()));
        }
        materialMapper.deleteByIds(ids);
        variantValueMapper.delete(Wrappers.<WmsVariantValue>lambdaQuery().in(WmsVariantValue::getMaterialId, ids));
    }

    private static void normalize(WmsMaterial material) {
        material.setBatchEnabled(Integer.valueOf(1).equals(material.getBatchEnabled()) ? 1 : 0);
        material.setSerialEnabled(Integer.valueOf(1).equals(material.getSerialEnabled()) ? 1 : 0);
        material.setIqcRequired(Integer.valueOf(1).equals(material.getIqcRequired()) ? 1 : 0);
        if (material.getCostMethod() == null) {
            material.setCostMethod(CostMethod.WEIGHTED);
        }
    }

    /** 条码不能与其他物料（含辅助单位的包装条码）重复；默认库位须属于默认仓库 */
    private void checkExtras(WmsMaterial material, Long id) {
        material.setBarcode(StringUtils.hasText(material.getBarcode()) ? material.getBarcode().trim() : null);
        if (material.getBarcode() != null) {
            WmsMaterial exist = Queries.first(materialMapper, Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getBarcode, material.getBarcode())
                    );
            boolean usedByUnit = unitMapper.exists(Wrappers.<WmsMaterialUnit>lambdaQuery().eq(WmsMaterialUnit::getBarcode, material.getBarcode()));
            if ((exist != null && !Objects.equals(exist.getId(), id)) || usedByUnit) {
                throw new BizException("wms.error.barcodeAlreadyUsedAnotherMaterialPackaging", material.getBarcode());
            }
        }
        if (material.getDefaultWarehouseId() == null) {
            material.setDefaultLocationId(null);
        } else if (material.getDefaultLocationId() != null) {
            WmsLocation location = locationMapper.selectById(material.getDefaultLocationId());
            if (location == null || !location.getWarehouseId().equals(material.getDefaultWarehouseId())) {
                throw new BizException("wms.error.defaultLocationDoesNotBelongDefault");
            }
        }
    }

    // ---------------------------------------------------------------- 辅助单位、条码

    public List<WmsMaterialUnit> units(Long materialId) {
        return unitMapper.selectList(Wrappers.<WmsMaterialUnit>lambdaQuery().eq(WmsMaterialUnit::getMaterialId, materialId)
                .orderByAsc(WmsMaterialUnit::getRatio));
    }

    /** 保存辅助单位（整体替换）。库存、成本都按基本单位，辅助单位只用于单据录入换算 */
    @org.springframework.transaction.annotation.Transactional
    public void saveUnits(Long materialId, List<WmsMaterialUnit> units) {
        WmsMaterial material = get(materialId);
        java.util.Set<String> names = new java.util.HashSet<>();
        for (WmsMaterialUnit u : units) {
            if (!StringUtils.hasText(u.getUnit()) || u.getUnit().trim().length() > 16) {
                throw new BizException("wms.error.alternateUnitNameRequiredCannotExceed");
            }
            u.setUnit(u.getUnit().trim());
            if (u.getUnit().equals(material.getUnit()) || !names.add(u.getUnit())) {
                throw new BizException("wms.error.duplicateAlternateUnit", u.getUnit());
            }
            if (u.getRatio() == null || u.getRatio().signum() <= 0) {
                throw new BizException("wms.error.conversionRatioMustGreater", u.getUnit());
            }
            u.setBarcode(StringUtils.hasText(u.getBarcode()) ? u.getBarcode().trim() : null);
            if (u.getBarcode() != null) {
                boolean used = materialMapper.exists(Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getBarcode, u.getBarcode()))
                        || unitMapper.exists(Wrappers.<WmsMaterialUnit>lambdaQuery().eq(WmsMaterialUnit::getBarcode, u.getBarcode())
                        .ne(WmsMaterialUnit::getMaterialId, materialId));
                if (used || units.stream().filter(o -> u.getBarcode().equals(o.getBarcode())).count() > 1) {
                    throw new BizException("wms.error.barcodeAlreadyUse", u.getBarcode());
                }
            }
        }
        unitMapper.delete(Wrappers.<WmsMaterialUnit>lambdaQuery().eq(WmsMaterialUnit::getMaterialId, materialId));
        for (WmsMaterialUnit u : units) {
            u.setId(null);
            u.setMaterialId(materialId);
            unitMapper.insert(u);
        }
    }

    /**
     * 扫码：按物料条码或包装条码查找启用的物料
     *
     * @return {material, unit, ratio}：包装条码时 unit、ratio 为辅助单位，否则为基本单位、1；找不到返回 null
     */
    public BarcodeMatch byBarcode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String c = code.trim();
        WmsMaterial m = Queries.first(materialMapper, Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getStatus, 1)
                .and(w -> w.eq(WmsMaterial::getBarcode, c).or().eq(WmsMaterial::getCode, c)));
        if (m != null) {
            return new BarcodeMatch(m, m.getUnit() == null ? "" : m.getUnit(), BigDecimal.ONE);
        }
        WmsMaterialUnit u = Queries.first(unitMapper, Wrappers.<WmsMaterialUnit>lambdaQuery().eq(WmsMaterialUnit::getBarcode, c));
        if (u == null) {
            return null;
        }
        m = materialMapper.selectById(u.getMaterialId());
        if (m == null || !Integer.valueOf(1).equals(m.getStatus())) {
            return null;
        }
        return new BarcodeMatch(m, u.getUnit(), u.getRatio());
    }

    private void checkCodeUnique(String code, Long id) {
        WmsMaterial exist = materialMapper.selectOne(Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("wms.error.materialCodeAlreadyExists");
        }
    }
}
