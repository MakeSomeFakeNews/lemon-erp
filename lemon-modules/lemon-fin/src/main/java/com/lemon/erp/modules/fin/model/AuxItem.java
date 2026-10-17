package com.lemon.erp.modules.fin.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 辅助核算项目：在科目上设置（可多选），手工凭证使用该科目时这些项目必填；分录上各项目可同时填写
 */
public enum AuxItem {
    DEPT,
    CUSTOMER,
    SUPPLIER,
    PROJECT,
    MATERIAL;

    /** 名称的消息键 */
    public String labelKey() {
        return "fin.enum.AuxItem." + name();
    }

    /** 科目上保存的辅助核算（逗号分隔的名称），忽略不认识的值 */
    public static Set<AuxItem> parse(String items) {
        Set<AuxItem> set = EnumSet.noneOf(AuxItem.class);
        if (items == null || items.isBlank()) {
            return set;
        }
        for (String s : items.split(",")) {
            Arrays.stream(values()).filter(i -> i.name().equals(s.trim())).findFirst().ifPresent(set::add);
        }
        return set;
    }

    /** 按固定顺序拼接为逗号分隔的名称，没有时为 null */
    public static String join(Collection<AuxItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(items).stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
