package com.lemon.erp.framework.data;

import lombok.Getter;

/**
 * 校验后的排序（{@link Sorts#resolve}）：field 一定在该报表的白名单中。
 * mapper XML 中用 {@code <choose>} 把 field 映射为 SQL 表达式（{@code <when test="sort.field == 'code'">m.code</when>}），
 * 方向写 {@code <if test="sort.desc"> DESC</if>}，不用 ${} 拼接请求中的值；排在默认排序之前，默认排序作为次序和分页的稳定依据
 */
@Getter
public final class Sort {

    private final String field;
    private final boolean desc;

    Sort(String field, boolean desc) {
        this.field = field;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return field + (desc ? " desc" : " asc");
    }
}
