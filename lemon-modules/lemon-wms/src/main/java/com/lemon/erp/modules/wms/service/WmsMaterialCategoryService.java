package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.dict.DictSource;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.wms.dto.MaterialCategoryRequests;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;
import com.lemon.erp.modules.wms.mapper.WmsMaterialCategoryMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.model.MaterialCategoryTree;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物料分类：多级树，每个租户一份（新租户从平台复制，见 {@link WmsTenantInitializer}）。
 * <ul>
 *   <li>物料（wms_material.category）以及检验方案、促销、返利项目、提成规则、凭证科目设置等都按分类<b>编码</b>关联，编码保存后不能修改；
 *       按分类筛选时包含下级分类（mapper XML 用 {@code WmsMaterialCategoryMapper.subtreeCodes}，Java 用 {@link #tree()}.subtree），
 *       按分类匹配规则时下级分类沿用上级的设置（{@link MaterialCategoryTree#lineage}，最近的上级优先）</li>
 *   <li>同一上级下名称不能重复，最多 {@value #MAX_DEPTH} 级；停用的分类（含其下级）不能再被新物料选用，已有物料不受影响</li>
 *   <li>有下级、被物料或其他单据引用、内置（程序依赖，如原材料、低值易耗品）的分类不能删除</li>
 * </ul>
 * 原来的业务字典 wms_material_category 已迁移到这里；作为 {@link DictSource} 继续以该类型提供「编码 → 完整路径」，
 * 前端的字典标签、按字典显示名称的报表不需要修改
 */
@Service
@RequiredArgsConstructor
public class WmsMaterialCategoryService implements DictSource {

    /** 字典类型（原业务字典，保留给按字典读取的地方） */
    public static final String DICT_TYPE = "wms_material_category";

    /** 最多层级 */
    public static final int MAX_DEPTH = 5;

    /** 自动生成的编码：C + 4 位流水 */
    private static final Pattern AUTO_CODE = Pattern.compile("^C(\\d{1,8})$");

    private final WmsMaterialCategoryMapper categoryMapper;
    private final WmsMaterialMapper materialMapper;
    private final ReferenceGuard referenceGuard;

    /** 当前租户的全部分类（含停用的） */
    public MaterialCategoryTree tree() {
        return MaterialCategoryTree.of(categoryMapper.selectList(Wrappers.<WmsMaterialCategory>lambdaQuery()
                .orderByAsc(WmsMaterialCategory::getSort).orderByAsc(WmsMaterialCategory::getId)));
    }

    /** 全部分类，按树的先序（上级在前、同级按排序），前端据 parentId 组成树 */
    public List<WmsMaterialCategory> list() {
        return tree().ordered();
    }

    /** 各分类直接包含的物料数（编码 → 数量，不含下级分类） */
    public Map<String, Long> materialCounts() {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : categoryMapper.countMaterials()) {
            map.put((String) row.get("code"), ((Number) row.get("count")).longValue());
        }
        return map;
    }

    public WmsMaterialCategory get(Long id) {
        WmsMaterialCategory c = id == null ? null : categoryMapper.selectById(id);
        if (c == null) {
            throw new BizException("wms.error.categoryNotFound");
        }
        return c;
    }

    // ---------------------------------------------------------------- 供其他功能使用

    @Override
    public String dictType() {
        return DICT_TYPE;
    }

    /** 可选用的分类：编码 → 完整路径（如「电子元器件 / 电阻」） */
    @Override
    public Map<String, String> options() {
        return tree().usableOptions();
    }

    /**
     * 保存物料等时校验分类：为空不校验；必须是存在且可选用（自身和上级都启用）的分类。
     * 修改时分类没有变化的不校验（分类停用后已有物料仍可保存）
     *
     * @param before 修改前的分类，新增时为 null
     */
    public void checkUsable(String code, String before) {
        if (!StringUtils.hasText(code) || code.equals(before)) {
            return;
        }
        MaterialCategoryTree tree = tree();
        if (tree.byCode(code) == null) {
            throw new BizException("wms.error.categoryCodeNotFound", code);
        }
        if (!tree.usable(code)) {
            throw new BizException("wms.error.categoryDisabled", tree.path(code));
        }
    }

    /**
     * 按文字匹配分类编码（导入、智能录入）：编码 → 完整路径（如「电子元器件/电阻」）→ 唯一的末级名称，规则见 {@link MaterialCategoryTree#resolve}
     *
     * @return 分类编码；文字为空返回 null；匹配不上报错
     */
    public String resolve(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String code = tree().resolve(text);
        if (code == null) {
            throw new BizException("wms.error.categoryTextNotFound", text.trim());
        }
        return code;
    }

    // ---------------------------------------------------------------- 维护

    /** @return 新分类 ID */
    @Transactional
    public Long create(MaterialCategoryRequests.Save req) {
        MaterialCategoryTree tree = tree();
        Long parentId = parentIdOf(req.parentId());
        WmsMaterialCategory parent = parentId == 0L ? null : requireParent(tree, parentId);
        if (parent != null && tree.depth(parent) >= MAX_DEPTH) {
            throw new BizException("wms.error.categoryTooDeep", MAX_DEPTH);
        }
        String name = req.name().trim();
        if (tree.nameTaken(parentId, name, null)) {
            throw new BizException("wms.error.categoryNameExists", name);
        }
        String code = StringUtils.hasText(req.code()) ? req.code().trim() : nextCode(tree);
        if (categoryMapper.exists(Wrappers.<WmsMaterialCategory>lambdaQuery().eq(WmsMaterialCategory::getCode, code))) {
            throw new BizException("wms.error.categoryCodeExists", code);
        }
        WmsMaterialCategory c = new WmsMaterialCategory();
        c.setParentId(parentId);
        c.setAncestors(tree.ancestorsUnder(parentId));
        c.setCode(code);
        c.setName(name);
        c.setSort(req.sort() != null ? req.sort() : tree.children(parentId).stream()
                .mapToInt(x -> x.getSort() == null ? 0 : x.getSort()).max().orElse(0) + 1);
        c.setStatus(req.status() == null ? EnableStatus.ENABLED : req.status());
        c.setBuiltin(0);
        c.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
        categoryMapper.insert(c);
        return c.getId();
    }

    /** 修改名称、上级、排序、状态、备注；编码不能修改 */
    @Transactional
    public void update(Long id, MaterialCategoryRequests.Save req) {
        MaterialCategoryTree tree = tree();
        WmsMaterialCategory c = tree.byId(id);
        if (c == null) {
            throw new BizException("wms.error.categoryNotFound");
        }
        Long parentId = parentIdOf(req.parentId());
        String name = req.name().trim();
        if (tree.nameTaken(parentId, name, id)) {
            throw new BizException("wms.error.categoryNameExists", name);
        }
        if (!Objects.equals(parentId, c.getParentId())) {
            move(tree, c, parentId);
        }
        WmsMaterialCategory update = new WmsMaterialCategory();
        update.setId(id);
        update.setName(name);
        update.setSort(req.sort() == null ? c.getSort() : req.sort());
        update.setStatus(req.status() == null ? c.getStatus() : req.status());
        categoryMapper.updateById(update);
        // 备注可清空：updateById 跳过 null
        categoryMapper.update(Wrappers.<WmsMaterialCategory>lambdaUpdate()
                .set(WmsMaterialCategory::getRemark, StringUtils.hasText(req.remark()) ? req.remark().trim() : null)
                .eq(WmsMaterialCategory::getId, id));
    }

    /** 拖动排序：ids 依次排在 parentId 下，不在该上级下的移动过来 */
    @Transactional
    public void sort(MaterialCategoryRequests.Sort req) {
        MaterialCategoryTree tree = tree();
        Long parentId = parentIdOf(req.parentId());
        if (parentId != 0L) {
            requireParent(tree, parentId);
        }
        if (new HashSet<>(req.ids()).size() != req.ids().size()) {
            throw new BizException("wms.error.categorySortInvalid");
        }
        int sort = 0;
        for (Long id : req.ids()) {
            WmsMaterialCategory c = tree.byId(id);
            if (c == null) {
                throw new BizException("wms.error.categoryNotFound");
            }
            if (!Objects.equals(parentId, c.getParentId())) {
                if (tree.nameTaken(parentId, c.getName(), id)) {
                    throw new BizException("wms.error.categoryNameExists", c.getName());
                }
                move(tree, c, parentId);
                // 同一请求中移动多个分类时，后面的校验按移动后的位置
                tree = tree();
            }
            WmsMaterialCategory update = new WmsMaterialCategory();
            update.setId(id);
            update.setSort(++sort);
            categoryMapper.updateById(update);
        }
    }

    /** 有下级、被物料或其他单据引用、内置的分类不能删除 */
    @Transactional
    public void delete(Long id) {
        MaterialCategoryTree tree = tree();
        WmsMaterialCategory c = tree.byId(id);
        if (c == null) {
            throw new BizException("wms.error.categoryNotFound");
        }
        String path = tree.path(c.getCode());
        if (Integer.valueOf(1).equals(c.getBuiltin())) {
            throw new BizException("wms.error.categoryBuiltinCannotDelete", path);
        }
        if (!tree.children(id).isEmpty()) {
            throw new BizException("wms.error.categoryHasChildren", path);
        }
        Long materials = materialMapper.selectCount(Wrappers.<WmsMaterial>lambdaQuery().eq(WmsMaterial::getCategory, c.getCode()));
        if (materials > 0) {
            throw new BizException("wms.error.categoryUsedByMaterials", path, materials);
        }
        referenceGuard.checkDeletable(RefType.MATERIAL_CATEGORY, id, Messages.get("wms.text.category", path));
        categoryMapper.deleteById(id);
    }

    /** 移动到新上级：不能移到自己或下级之下、不能超过层级上限；同时更新全部下级的祖级路径 */
    private void move(MaterialCategoryTree tree, WmsMaterialCategory c, Long parentId) {
        if (tree.isSelfOrDescendant(c, parentId)) {
            throw new BizException("wms.error.categoryMoveIntoSelf");
        }
        WmsMaterialCategory parent = parentId == 0L ? null : requireParent(tree, parentId);
        int newDepth = (parent == null ? 0 : tree.depth(parent)) + 1;
        if (newDepth + tree.height(c) > MAX_DEPTH) {
            throw new BizException("wms.error.categoryTooDeep", MAX_DEPTH);
        }
        String oldPrefix = c.getAncestors() + "," + c.getId();
        String ancestors = tree.ancestorsUnder(parentId);
        String newPrefix = ancestors + "," + c.getId();
        WmsMaterialCategory update = new WmsMaterialCategory();
        update.setId(c.getId());
        update.setParentId(parentId);
        update.setAncestors(ancestors);
        categoryMapper.updateById(update);
        for (WmsMaterialCategory d : tree.descendants(c)) {
            WmsMaterialCategory du = new WmsMaterialCategory();
            du.setId(d.getId());
            du.setAncestors(newPrefix + d.getAncestors().substring(oldPrefix.length()));
            categoryMapper.updateById(du);
        }
    }

    private static Long parentIdOf(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private static WmsMaterialCategory requireParent(MaterialCategoryTree tree, Long parentId) {
        WmsMaterialCategory parent = tree.byId(parentId);
        if (parent == null) {
            throw new BizException("wms.error.categoryParentNotFound");
        }
        return parent;
    }

    /** 自动编码：已有 C + 数字编码的最大值 + 1，至少 4 位 */
    private static String nextCode(MaterialCategoryTree tree) {
        long max = 0;
        Set<String> codes = new HashSet<>();
        for (WmsMaterialCategory c : tree.ordered()) {
            codes.add(c.getCode());
            Matcher m = AUTO_CODE.matcher(c.getCode());
            if (m.matches()) {
                max = Math.max(max, Long.parseLong(m.group(1)));
            }
        }
        String code;
        do {
            code = String.format("C%04d", ++max);
        } while (codes.contains(code));
        return code;
    }
}
