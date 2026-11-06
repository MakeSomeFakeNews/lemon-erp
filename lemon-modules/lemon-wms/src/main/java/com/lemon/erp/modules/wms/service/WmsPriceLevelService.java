package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.wms.entity.WmsPriceLevel;
import com.lemon.erp.modules.wms.mapper.WmsPriceLevelMapper;
import com.lemon.erp.modules.wms.model.PriceLevels;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 价格列设置：零售价、批发价1-8 的名称和是否启用。没有设置过的列按默认（零售价、批发价1-3 启用）
 */
@Service
@RequiredArgsConstructor
public class WmsPriceLevelService {

    /** 默认启用的价格列：零售价、批发价1-3 */
    private static final int DEFAULT_ENABLED_MAX = 3;
    private static final int NAME_MAX = 32;

    private final WmsPriceLevelMapper levelMapper;

    /** 默认名称（中文，存库） */
    public static String defaultName(int level) {
        return level == PriceLevels.RETAIL ? Messages.get(Messages.DEFAULT_LOCALE, "wms.text.priceLevelRetail")
                : Messages.get(Messages.DEFAULT_LOCALE, "wms.text.priceLevelWholesale", String.valueOf(level));
    }

    /** 显示名称：未改过的默认名称按当前语言显示 */
    private static String displayName(int level, String name) {
        if (!defaultName(level).equals(name)) {
            return name;
        }
        return level == PriceLevels.RETAIL ? Messages.get("wms.text.priceLevelRetail")
                : Messages.get("wms.text.priceLevelWholesale", String.valueOf(level));
    }

    /** 全部 9 个价格列（按 level 排序），名称为显示名称 */
    public List<WmsPriceLevel> list() {
        Map<Integer, WmsPriceLevel> saved = levelMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(WmsPriceLevel::getLevel, Function.identity(), (a, b) -> a));
        List<WmsPriceLevel> list = new ArrayList<>(PriceLevels.MAX + 1);
        for (int level = PriceLevels.RETAIL; level <= PriceLevels.MAX; level++) {
            WmsPriceLevel l = saved.get(level);
            if (l == null) {
                l = defaults(level);
            }
            l.setName(displayName(level, l.getName()));
            list.add(l);
        }
        return list;
    }

    /** 启用的价格列 */
    public Set<Integer> enabledLevels() {
        return list().stream().filter(l -> l.getStatus() == EnableStatus.ENABLED).map(WmsPriceLevel::getLevel).collect(Collectors.toSet());
    }

    public boolean isEnabled(int level) {
        return enabledLevels().contains(level);
    }

    /** 保存价格列设置：按 level 更新名称和状态，没有的新建 */
    @Transactional
    public void save(List<WmsPriceLevel> levels) {
        Set<Integer> seen = new HashSet<>();
        for (WmsPriceLevel l : levels) {
            if (!PriceLevels.valid(l.getLevel()) || !seen.add(l.getLevel())) {
                throw new BizException("wms.error.priceLevelInvalid");
            }
            if (!StringUtils.hasText(l.getName()) || l.getName().trim().length() > NAME_MAX) {
                throw new BizException("wms.error.priceLevelNameRequired", String.valueOf(NAME_MAX));
            }
        }
        Map<Integer, WmsPriceLevel> saved = levelMapper.selectList(Wrappers.emptyWrapper()).stream()
                .collect(Collectors.toMap(WmsPriceLevel::getLevel, Function.identity(), (a, b) -> a));
        for (WmsPriceLevel l : levels) {
            String name = l.getName().trim();
            int level = l.getLevel();
            // 当前语言的默认名称保存为中文默认名称，切换语言后仍按语言显示
            if (name.equals(displayName(level, defaultName(level)))) {
                name = defaultName(level);
            }
            EnableStatus status = l.getStatus() == null ? EnableStatus.ENABLED : l.getStatus();
            WmsPriceLevel old = saved.get(level);
            if (old == null) {
                WmsPriceLevel n = new WmsPriceLevel();
                n.setLevel(level);
                n.setName(name);
                n.setStatus(status);
                levelMapper.insert(n);
            } else {
                old.setName(name);
                old.setStatus(status);
                levelMapper.updateById(old);
            }
        }
    }

    /** 新租户的价格列：按默认设置 */
    void initDefaults() {
        for (int level = PriceLevels.RETAIL; level <= PriceLevels.MAX; level++) {
            levelMapper.insert(defaults(level));
        }
    }

    private static WmsPriceLevel defaults(int level) {
        WmsPriceLevel l = new WmsPriceLevel();
        l.setLevel(level);
        l.setName(defaultName(level));
        l.setStatus(level <= DEFAULT_ENABLED_MAX ? EnableStatus.ENABLED : EnableStatus.DISABLED);
        return l;
    }
}
