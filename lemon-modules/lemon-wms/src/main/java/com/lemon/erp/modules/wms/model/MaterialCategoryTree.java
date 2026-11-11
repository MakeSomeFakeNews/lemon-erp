package com.lemon.erp.modules.wms.model;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.wms.entity.WmsMaterialCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 某个租户全部物料分类的快照（一次查询加载），提供上级链、下级、完整路径和按名称匹配。
 * 物料等按分类编码关联，编码不在分类中（如已删除）时按只有自身的一级分类处理
 */
public final class MaterialCategoryTree {

    /** 完整路径名称的分隔符，如「电子元器件 / 电阻」；导入、匹配时「/」两侧的空格可有可无 */
    public static final String PATH_SEPARATOR = " / ";

    private static final Comparator<WmsMaterialCategory> ORDER = Comparator
            .comparing((WmsMaterialCategory c) -> c.getSort() == null ? 0 : c.getSort())
            .thenComparing(WmsMaterialCategory::getId);

    private final Map<Long, WmsMaterialCategory> byId = new HashMap<>();
    private final Map<String, WmsMaterialCategory> byCode = new HashMap<>();
    private final Map<Long, List<WmsMaterialCategory>> children = new HashMap<>();

    private MaterialCategoryTree(List<WmsMaterialCategory> all) {
        for (WmsMaterialCategory c : all) {
            byId.put(c.getId(), c);
            byCode.put(c.getCode(), c);
        }
        for (WmsMaterialCategory c : all) {
            // 上级不存在（数据异常）时作为一级分类
            long parent = c.getParentId() != null && byId.containsKey(c.getParentId()) ? c.getParentId() : 0L;
            children.computeIfAbsent(parent, k -> new ArrayList<>()).add(c);
        }
        children.values().forEach(list -> list.sort(ORDER));
    }

    public static MaterialCategoryTree of(List<WmsMaterialCategory> all) {
        return new MaterialCategoryTree(all);
    }

    public WmsMaterialCategory byCode(String code) {
        return code == null ? null : byCode.get(code);
    }

    public WmsMaterialCategory byId(Long id) {
        return id == null ? null : byId.get(id);
    }

    /** 直接下级，按排序 */
    public List<WmsMaterialCategory> children(Long id) {
        return children.getOrDefault(id == null ? 0L : id, List.of());
    }

    /** 上级分类（一级分类为 null） */
    public WmsMaterialCategory parent(WmsMaterialCategory c) {
        return c == null || c.getParentId() == null ? null : byId.get(c.getParentId());
    }

    /** 分类自身及各级上级的编码，自身在前（用于「按上级分类设置的规则对下级分类同样适用」）；编码不在分类中时只有自身，为空时为空列表 */
    public List<String> lineage(String code) {
        if (code == null || code.isBlank()) {
            return List.of();
        }
        WmsMaterialCategory c = byCode.get(code);
        if (c == null) {
            return List.of(code);
        }
        List<String> codes = new ArrayList<>();
        for (int guard = 0; c != null && guard < 64; guard++, c = parent(c)) {
            codes.add(c.getCode());
        }
        return codes;
    }

    /** 分类自身及全部下级的编码 */
    public List<String> subtree(String code) {
        WmsMaterialCategory c = byCode(code);
        if (c == null) {
            return code == null || code.isBlank() ? List.of() : List.of(code);
        }
        List<String> codes = new ArrayList<>();
        collect(c, codes, 0);
        return codes;
    }

    private void collect(WmsMaterialCategory c, List<String> codes, int depth) {
        codes.add(c.getCode());
        if (depth < 64) {
            children(c.getId()).forEach(child -> collect(child, codes, depth + 1));
        }
    }

    /** 全部下级分类（不含自身） */
    public List<WmsMaterialCategory> descendants(WmsMaterialCategory c) {
        List<WmsMaterialCategory> list = new ArrayList<>();
        for (String code : subtree(c.getCode())) {
            if (!code.equals(c.getCode())) {
                list.add(byCode.get(code));
            }
        }
        return list;
    }

    /** 层级，一级分类为 1 */
    public int depth(WmsMaterialCategory c) {
        return lineage(c.getCode()).size();
    }

    /** 下级的最大层数（没有下级为 0） */
    public int height(WmsMaterialCategory c) {
        int max = 0;
        for (WmsMaterialCategory child : children(c.getId())) {
            max = Math.max(max, height(child) + 1);
        }
        return max;
    }

    /** 完整路径名称，如「电子元器件 / 电阻」；编码不在分类中时为编码本身 */
    public String path(String code) {
        WmsMaterialCategory c = byCode(code);
        if (c == null) {
            return code;
        }
        List<String> names = new ArrayList<>();
        for (String x : lineage(code)) {
            names.add(0, byCode.get(x).getName());
        }
        return String.join(PATH_SEPARATOR, names);
    }

    /** 自身和各级上级都启用（可以被新物料选用） */
    public boolean usable(String code) {
        WmsMaterialCategory c = byCode(code);
        if (c == null) {
            return false;
        }
        for (String x : lineage(code)) {
            if (byCode.get(x).getStatus() != EnableStatus.ENABLED) {
                return false;
            }
        }
        return true;
    }

    /** 可选用的分类（自身和上级都启用）：编码 → 完整路径，按树的先序（上级在前、同级按排序） */
    public Map<String, String> usableOptions() {
        Map<String, String> map = new LinkedHashMap<>();
        walk(0L, map, 0);
        return map;
    }

    private void walk(Long parentId, Map<String, String> map, int depth) {
        if (depth >= 64) {
            return;
        }
        for (WmsMaterialCategory c : children(parentId)) {
            if (c.getStatus() == EnableStatus.ENABLED) {
                map.put(c.getCode(), path(c.getCode()));
                walk(c.getId(), map, depth + 1);
            }
        }
    }

    /**
     * 按文字匹配分类编码（导入、智能录入、数据整理时使用），依次尝试：
     * 分类编码（忽略大小写）→ 完整路径（如「电子元器件/电阻」，「/」两侧空格可有可无）→ 末级名称（只有一个分类叫这个名称时）。
     * 匹配不上或末级名称有重复时返回 null
     */
    public String resolve(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        for (WmsMaterialCategory c : byId.values()) {
            if (c.getCode().equalsIgnoreCase(t)) {
                return c.getCode();
            }
        }
        String normalized = normalizePath(t);
        for (WmsMaterialCategory c : byId.values()) {
            if (normalizePath(path(c.getCode())).equals(normalized)) {
                return c.getCode();
            }
        }
        List<WmsMaterialCategory> sameName = byId.values().stream().filter(c -> c.getName().equals(t)).toList();
        return sameName.size() == 1 ? sameName.get(0).getCode() : null;
    }

    private static String normalizePath(String path) {
        String[] parts = path.split("/");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            list.add(p.trim().toLowerCase(Locale.ROOT));
        }
        return String.join("/", list);
    }

    /** 新上级是否在 c 的下级中（或就是 c 自身），移动到这里会形成环 */
    public boolean isSelfOrDescendant(WmsMaterialCategory c, Long newParentId) {
        if (newParentId == null || newParentId == 0L) {
            return false;
        }
        WmsMaterialCategory p = byId.get(newParentId);
        return p != null && lineage(p.getCode()).contains(c.getCode());
    }

    /** 分类的祖级 ID 路径（作为 parentId 的下级时） */
    public String ancestorsUnder(Long parentId) {
        WmsMaterialCategory p = byId(parentId);
        return p == null ? "0" : p.getAncestors() + "," + p.getId();
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }

    /** 全部分类，按树的先序 */
    public List<WmsMaterialCategory> ordered() {
        List<WmsMaterialCategory> list = new ArrayList<>();
        order(0L, list, 0);
        return list;
    }

    private void order(Long parentId, List<WmsMaterialCategory> list, int depth) {
        if (depth >= 64) {
            return;
        }
        for (WmsMaterialCategory c : children(parentId)) {
            list.add(c);
            order(c.getId(), list, depth + 1);
        }
    }

    /** 同一上级下是否已有该名称的分类 */
    public boolean nameTaken(Long parentId, String name, Long excludeId) {
        return children(parentId).stream().anyMatch(c -> c.getName().equals(name) && !Objects.equals(c.getId(), excludeId));
    }
}
