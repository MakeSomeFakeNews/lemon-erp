package com.lemon.erp.common.util;

import com.lemon.erp.common.base.TreeEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 树形结构工具
 */
public final class TreeUtils {

    private static final Comparator<TreeEntity<?>> BY_SORT =
            Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort());

    private TreeUtils() {
    }

    /**
     * 将平铺列表按 sort 组装为树；父节点不在列表中的节点作为根节点
     * （因此只查询部分节点时，例如按数据权限过滤后，也能得到正确的子树）
     */
    public static <T extends TreeEntity<T>> List<T> build(Collection<T> nodes) {
        Map<Long, T> byId = nodes.stream().collect(Collectors.toMap(TreeEntity::getId, Function.identity(), (a, b) -> a));
        List<T> sorted = nodes.stream().sorted(BY_SORT).toList();
        sorted.forEach(n -> n.setChildren(new ArrayList<>()));
        List<T> roots = new ArrayList<>();
        for (T node : sorted) {
            T parent = byId.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 在平铺列表中查找节点自身及全部子孙节点的 ID（包含 rootId 本身）
     */
    public static Set<Long> selfAndDescendantIds(Collection<? extends TreeEntity<?>> nodes, Long rootId) {
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        nodes.forEach(n -> childrenByParent.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(n.getId()));
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(Objects.requireNonNull(rootId));
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            // add 返回 false 说明已访问过，脏数据成环时也不会死循环
            if (result.add(id)) {
                queue.addAll(childrenByParent.getOrDefault(id, List.of()));
            }
        }
        return result;
    }
}
