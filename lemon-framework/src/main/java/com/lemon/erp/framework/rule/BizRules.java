package com.lemon.erp.framework.rule;

import java.math.BigDecimal;
import java.util.List;

/**
 * 读取当前租户的业务参数（由系统模块实现，按租户缓存）
 */
public interface BizRules {

    /** 参数值，未修改时为默认值；参数未登记时抛异常 */
    String get(String key);

    default boolean bool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    default int number(String key) {
        return Integer.parseInt(get(key));
    }

    default BigDecimal decimal(String key) {
        return new BigDecimal(get(key));
    }

    /** 表格型参数：每行一个列表，留空的单元格为 null */
    default List<List<BigDecimal>> table(String key) {
        return BizRule.parseTable(get(key));
    }

    default <E extends Enum<E>> E option(String key, Class<E> type) {
        return Enum.valueOf(type, get(key));
    }
}
