package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.framework.data.SearchKeyTable;
import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.wms.model.PriceLevels;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WmsMaterialService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("code", "物料编码", "name", "物料名称", "spec", "规格型号", "unit", "计量单位", "category", "分类", "brand", "品牌", // i18n-ignore 字段名写入变更记录
            "batchEnabled", "批次管理", "serialEnabled", "序列号管理", "iqcRequired", "来料检验", "safetyStock", "安全库存", "reorderPoint", "再订货点", "reorderQty", "再订货量", "barcode", "条码", "defaultWarehouseId", "默认仓库", "refCost", "参考成本价", "costMethod", "计价方法", "remark", "备注", // i18n-ignore
            "retailPrice", "零售价", "wholesalePrice1", "批发价1", "wholesalePrice2", "批发价2", "wholesalePrice3", "批发价3", // i18n-ignore
            "wholesalePrice4", "批发价4", "wholesalePrice5", "批发价5", "wholesalePrice6", "批发价6", "wholesalePrice7", "批发价7", // i18n-ignore
            "wholesalePrice8", "批发价8"); // i18n-ignore

    /** 品牌字典（业务字典） */
    public static final String BRAND_DICT = "wms_material_brand";

    /** 下拉搜索每页最多返回的条数 */
    public static final int OPTION_LIMIT = 50;

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
    private final DictLookup dictLookup;
    private final WmsPriceLevelService priceLevelService;
    private final WmsMaterialCategoryService categoryService;
    private final SearchKeyTable<WmsMaterial> searchKeys;

    /**
     * @param keyword    编码、名称、规格、条码、拼音首字母或去掉分隔符的缩写，规则见 {@link SearchKeys}
     * @param templateId 只看某个款式的 SKU
     */
    public PageResult<WmsMaterial> page(long current, long size, String keyword, String category, EnableStatus status, Long templateId) {
        return page(current, size, keyword, category, null, status, templateId);
    }

    /**
     * @param category 分类编码，包含其下级分类的物料；为空不限
     * @param brand    品牌，为空不限
     */
    public PageResult<WmsMaterial> page(long current, long size, String keyword, String category, String brand, EnableStatus status, Long templateId) {
        Page<WmsMaterial> page = materialMapper.selectPage(Page.of(current, size), Wrappers.<WmsMaterial>lambdaQuery()
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .in(StringUtils.hasText(category), WmsMaterial::getCategory, categoryCodes(category))
                .eq(StringUtils.hasText(brand), WmsMaterial::getBrand, brand)
                .eq(status != null, WmsMaterial::getStatus, status)
                .eq(templateId != null, WmsMaterial::getTemplateId, templateId)
                .orderByDesc(WmsMaterial::getId));
        return PageResult.of(page);
    }

    /** 分类及其下级分类的编码（按分类筛选时使用），为空时不查询 */
    private List<String> categoryCodes(String category) {
        return StringUtils.hasText(category) ? categoryService.tree().subtree(category) : List.of();
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
            if (material.getStatus() != EnableStatus.ENABLED) {
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

    /** 启用的物料，按关键字搜索，最多 {@value #OPTION_LIMIT} 条，供单据明细选择、AI 识别物料 */
    public List<WmsMaterial> options(String keyword) {
        return options(keyword, 1, OPTION_LIMIT);
    }

    /**
     * 启用的物料下拉：按关键字（规则见 {@link SearchKeys}）分页搜索，按编码排序，不查总数；
     * 第一页中编码或条码与关键字完全一致的物料排在最前
     *
     * @param size 每页条数，最多 {@value #OPTION_LIMIT}
     */
    public List<WmsMaterial> options(String keyword, long current, long size) {
        List<WmsMaterial> rows = materialMapper.selectPage(Page.of(Math.max(current, 1), Math.min(Math.max(size, 1), OPTION_LIMIT), false),
                optionQuery().eq(WmsMaterial::getStatus, EnableStatus.ENABLED)
                        // 多规格款式不能直接用于单据，选择具体的 SKU
                        .eq(WmsMaterial::getIsTemplate, 0)
                        .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                        .orderByAsc(WmsMaterial::getCode)).getRecords();
        if (!StringUtils.hasText(keyword) || current > 1) {
            return rows;
        }
        String kw = keyword.trim();
        WmsMaterial exact = Queries.first(materialMapper, optionQuery().eq(WmsMaterial::getStatus, EnableStatus.ENABLED)
                .eq(WmsMaterial::getIsTemplate, 0)
                .and(w -> w.eq(WmsMaterial::getCode, kw).or().eq(WmsMaterial::getBarcode, kw))
                .orderByAsc(WmsMaterial::getCode));
        return SearchKeys.exactFirst(rows, exact, WmsMaterial::getId);
    }

    /** 按 ID 取物料（含停用的，最多 {@value #OPTION_LIMIT} 个，列同下拉），下拉回显已选的值 */
    public List<WmsMaterial> optionsByIds(List<Long> ids) {
        return ids.isEmpty() ? List.of() : materialMapper.selectList(optionQuery()
                .in(WmsMaterial::getId, ids.stream().distinct().limit(OPTION_LIMIT).toList()));
    }

    /** 下拉返回的列 */
    private static LambdaQueryWrapper<WmsMaterial> optionQuery() {
        return Wrappers.<WmsMaterial>lambdaQuery()
                .select(WmsMaterial::getId, WmsMaterial::getCode, WmsMaterial::getName, WmsMaterial::getSpec,
                        WmsMaterial::getUnit, WmsMaterial::getBatchEnabled, WmsMaterial::getSerialEnabled,
                        WmsMaterial::getBarcode, WmsMaterial::getDefaultWarehouseId, WmsMaterial::getDefaultLocationId);
    }

    /**
     * 数据整理的候选物料：启用的物料（不含多规格的 SKU，SKU 由款式生成、名称相同），按 ID 升序分页，只查分析需要的列
     *
     * @param keyword 关键字，规则见 {@link SearchKeys}
     */
    public PageResult<WmsMaterial> pageForCleanse(long current, long size, String keyword) {
        Page<WmsMaterial> page = materialMapper.selectPage(Page.of(current, size), Wrappers.<WmsMaterial>lambdaQuery()
                .select(WmsMaterial::getId, WmsMaterial::getCode, WmsMaterial::getName, WmsMaterial::getSpec,
                        WmsMaterial::getCategory, WmsMaterial::getUnit, WmsMaterial::getBarcode)
                .eq(WmsMaterial::getStatus, EnableStatus.ENABLED)
                .isNull(WmsMaterial::getTemplateId)
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .orderByAsc(WmsMaterial::getId));
        return PageResult.of(page);
    }

    /** 关键字匹配：编码、名称、规格、条码包含关键字，或检索列包含规范化的关键字（{@link SearchKeys}） */
    public static Consumer<LambdaQueryWrapper<WmsMaterial>> keywordMatch(String keyword) {
        return SearchKeys.matches(keyword, WmsMaterial::getSearchKey,
                WmsMaterial::getCode, WmsMaterial::getName, WmsMaterial::getSpec, WmsMaterial::getBarcode);
    }

    /** 是否有其他启用的物料名称和规格都相同（规格为空视为相同的空规格） */
    public boolean existsSameNameSpec(String name, String spec, Long excludeId) {
        return materialMapper.exists(Wrappers.<WmsMaterial>lambdaQuery()
                .eq(WmsMaterial::getName, name)
                .and(w -> {
                    if (StringUtils.hasText(spec)) {
                        w.eq(WmsMaterial::getSpec, spec);
                    } else {
                        w.isNull(WmsMaterial::getSpec).or().eq(WmsMaterial::getSpec, "");
                    }
                })
                .eq(WmsMaterial::getStatus, EnableStatus.ENABLED)
                .ne(excludeId != null, WmsMaterial::getId, excludeId));
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
        categoryService.checkUsable(material.getCategory(), null);
        if (material.getStatus() == null) material.setStatus(EnableStatus.ENABLED);
        materialMapper.insert(material);
    }

    /**
     * 导入一行：分类可填写分类编码、完整路径（如「电子元器件/电阻」）或不重名的分类名称，规则见 {@link WmsMaterialCategoryService#resolve}；
     * 其余同新增
     */
    @Transactional
    public void importRow(WmsMaterial material) {
        material.setCategory(categoryService.resolve(material.getCategory()));
        create(material);
    }

    /** 已发生库存业务的物料，不能修改批次、序列号管理方式，否则已有库存与新规则不一致 */
    @Transactional
    public void update(Long id, WmsMaterial material) {
        Object before = materialMapper.selectById(id);
        WmsMaterial old = get(id);
        checkCodeUnique(material.getCode(), id);
        normalize(material);
        checkExtras(material, id);
        categoryService.checkUsable(material.getCategory(), old.getCategory());
        boolean trackingChanged = !Objects.equals(old.getBatchEnabled(), material.getBatchEnabled())
                || !Objects.equals(old.getSerialEnabled(), material.getSerialEnabled());
        if (trackingChanged && flowMapper.existsByMaterialId(id)) {
            throw new BizException("wms.error.materialHasStockTransactionsBatchSerial");
        }
        // 停用的价格列在表单中隐藏，保留原值
        java.util.Set<Integer> enabledLevels = priceLevelService.enabledLevels();
        for (int level = PriceLevels.RETAIL; level <= PriceLevels.MAX; level++) {
            if (!enabledLevels.contains(level)) {
                PriceLevels.setPrice(material, level, PriceLevels.price(old, level));
            }
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
                .set(WmsMaterial::getBrand, material.getBrand())
                .set(WmsMaterial::getCategory, material.getCategory())
                .set(WmsMaterial::getRetailPrice, material.getRetailPrice())
                .set(WmsMaterial::getWholesalePrice1, material.getWholesalePrice1())
                .set(WmsMaterial::getWholesalePrice2, material.getWholesalePrice2())
                .set(WmsMaterial::getWholesalePrice3, material.getWholesalePrice3())
                .set(WmsMaterial::getWholesalePrice4, material.getWholesalePrice4())
                .set(WmsMaterial::getWholesalePrice5, material.getWholesalePrice5())
                .set(WmsMaterial::getWholesalePrice6, material.getWholesalePrice6())
                .set(WmsMaterial::getWholesalePrice7, material.getWholesalePrice7())
                .set(WmsMaterial::getWholesalePrice8, material.getWholesalePrice8())
                .eq(WmsMaterial::getId, id));
        WmsMaterial after = materialMapper.selectById(id);
        searchKeys.refresh(after);
        changeLogger.log("wms_material", id, before, after, CHANGE_FIELDS);
    }

    public void changeStatus(Long id, EnableStatus status) {
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
        material.setCategory(StringUtils.hasText(material.getCategory()) ? material.getCategory().trim() : null);
        material.setBatchEnabled(Integer.valueOf(1).equals(material.getBatchEnabled()) ? 1 : 0);
        material.setSerialEnabled(Integer.valueOf(1).equals(material.getSerialEnabled()) ? 1 : 0);
        material.setIqcRequired(Integer.valueOf(1).equals(material.getIqcRequired()) ? 1 : 0);
        if (material.getCostMethod() == null) {
            material.setCostMethod(CostMethod.WEIGHTED);
        }
    }

    /** 条码不能与其他物料（含辅助单位的包装条码）重复；默认库位须属于默认仓库；品牌须为业务字典中的值 */
    private void checkExtras(WmsMaterial material, Long id) {
        material.setBrand(StringUtils.hasText(material.getBrand()) ? material.getBrand().trim() : null);
        dictLookup.check(BRAND_DICT, material.getBrand(), "wms.label.brand");
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

    // ---------------------------------------------------------------- 商品价格批量维护

    /**
     * 商品价格列表：可用于单据的物料（不含多规格款式）及各价格列，按编码排序
     *
     * @param keyword 关键字（含条码），规则见 {@link SearchKeys}
     */
    public PageResult<WmsMaterial> pagePrices(long current, long size, String keyword, String category, String brand, EnableStatus status) {
        Page<WmsMaterial> page = materialMapper.selectPage(Page.of(current, size), Wrappers.<WmsMaterial>lambdaQuery()
                .select(WmsMaterial::getId, WmsMaterial::getCode, WmsMaterial::getName, WmsMaterial::getSpec, WmsMaterial::getUnit,
                        WmsMaterial::getCategory, WmsMaterial::getBrand, WmsMaterial::getRefCost, WmsMaterial::getStatus,
                        WmsMaterial::getRetailPrice, WmsMaterial::getWholesalePrice1, WmsMaterial::getWholesalePrice2,
                        WmsMaterial::getWholesalePrice3, WmsMaterial::getWholesalePrice4, WmsMaterial::getWholesalePrice5,
                        WmsMaterial::getWholesalePrice6, WmsMaterial::getWholesalePrice7, WmsMaterial::getWholesalePrice8)
                .eq(WmsMaterial::getIsTemplate, 0)
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .in(StringUtils.hasText(category), WmsMaterial::getCategory, categoryCodes(category))
                .eq(StringUtils.hasText(brand), WmsMaterial::getBrand, brand)
                .eq(status != null, WmsMaterial::getStatus, status)
                .orderByAsc(WmsMaterial::getCode));
        return PageResult.of(page);
    }

    /**
     * 批量保存商品价格。每行为 物料 id + 要修改的价格列（字段名见 {@link PriceLevels#FIELDS}）：
     * <b>只更新行中出现的字段</b>，没有出现的价格列保持不变；要清空某列须显式传 null（或空字符串）。
     * 停用的价格列和其他字段忽略
     *
     * @return 价格有变化的物料数
     */
    @Transactional
    public int savePrices(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        java.util.Set<Integer> enabledLevels = priceLevelService.enabledLevels();
        List<Long> ids = new java.util.ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Long id = rows.get(i) == null ? null : rowId(rows.get(i).get("id"));
            if (id == null) {
                throw new BizException("wms.error.priceRowIdRequired", i + 1);
            }
            ids.add(id);
        }
        Map<Long, WmsMaterial> exists = mapByIds(ids);
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            WmsMaterial old = exists.get(ids.get(i));
            if (old == null) {
                throw new BizException("wms.error.materialNotFound2", ids.get(i));
            }
            var update = Wrappers.<WmsMaterial>lambdaUpdate().eq(WmsMaterial::getId, old.getId())
                    .set(WmsMaterial::getUpdateTime, java.time.LocalDateTime.now());
            boolean changed = false;
            for (int level = PriceLevels.RETAIL; level <= PriceLevels.MAX; level++) {
                String field = PriceLevels.FIELDS.get(level);
                if (!enabledLevels.contains(level) || !row.containsKey(field)) {
                    continue;
                }
                BigDecimal price = priceValue(row.get(field), old.getCode());
                BigDecimal before = PriceLevels.price(old, level);
                if (before == null ? price == null : price != null && before.compareTo(price) == 0) {
                    continue;
                }
                changed = true;
                update.set(PRICE_COLUMNS.get(level), price);
                // 同一请求中同一物料出现多次时按最后一次
                PriceLevels.setPrice(old, level, price);
            }
            if (changed) {
                materialMapper.update(update);
                count++;
            }
        }
        return count;
    }

    private static Long rowId(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s && s.matches("\\d{1,18}")) {
            return Long.valueOf(s);
        }
        return null;
    }

    /** 价格字段的值：null / 空字符串为清空，数字或数字字符串，不能小于 0，保留 4 位小数 */
    private static BigDecimal priceValue(Object v, String materialCode) {
        if (v == null || v instanceof String s && s.isBlank()) {
            return null;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new BizException("wms.error.priceInvalid", materialCode, v);
        }
        if (price.signum() < 0) {
            throw new BizException("wms.error.priceNegative", materialCode);
        }
        return price.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /** 价格列 → 物料字段 */
    private static final List<com.baomidou.mybatisplus.core.toolkit.support.SFunction<WmsMaterial, ?>> PRICE_COLUMNS = List.of(
            WmsMaterial::getRetailPrice, WmsMaterial::getWholesalePrice1, WmsMaterial::getWholesalePrice2, WmsMaterial::getWholesalePrice3,
            WmsMaterial::getWholesalePrice4, WmsMaterial::getWholesalePrice5, WmsMaterial::getWholesalePrice6, WmsMaterial::getWholesalePrice7,
            WmsMaterial::getWholesalePrice8);

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
        WmsMaterial m = Queries.first(materialMapper, Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getStatus, EnableStatus.ENABLED)
                .and(w -> w.eq(WmsMaterial::getBarcode, c).or().eq(WmsMaterial::getCode, c)));
        if (m != null) {
            return new BarcodeMatch(m, m.getUnit() == null ? "" : m.getUnit(), BigDecimal.ONE);
        }
        WmsMaterialUnit u = Queries.first(unitMapper, Wrappers.<WmsMaterialUnit>lambdaQuery().eq(WmsMaterialUnit::getBarcode, c));
        if (u == null) {
            return null;
        }
        m = materialMapper.selectById(u.getMaterialId());
        if (m == null || m.getStatus() != EnableStatus.ENABLED) {
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
