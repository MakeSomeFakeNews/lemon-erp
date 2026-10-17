package com.lemon.erp.modules.system.dto;

import java.util.List;

/**
 * 功能模块与行业模板
 *
 * @param features   功能模块，menuIds 为模块包含的菜单（不含上级目录）
 * @param industries 行业模板，features 已包含依赖的模块和基础平台
 */
public record FeatureCatalogView(List<Feature> features, List<Industry> industries) {

    public record Feature(String key, String name, String group, String description, boolean required,
                          List<String> requires, List<Long> menuIds) {
    }

    public record Industry(String key, String name, String description, List<String> features) {
    }
}
