package com.lemon.erp.framework.data;

import com.lemon.erp.common.exception.BizException;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 列头排序的白名单校验（issue #122）：只接受报表声明的字段，其他字段（包括没有权限、不返回的金额列）报错，
 * 防止按任意列排序和 SQL 注入。白名单用 {@link #fields} 声明，受权限控制的列用 {@link Fields#addIf} 追加
 */
public final class Sorts {

    private static final String ASC = "asc";
    private static final String DESC = "desc";

    private Sorts() {
    }

    /**
     * @param param   请求中的排序参数，可为空
     * @param allowed 可排序的字段（前端的列字段名）
     * @return 校验后的排序；没有指定排序字段时为 null（按默认顺序）
     * @throws BizException 字段不在白名单中，或方向不是 asc / desc
     */
    public static Sort resolve(SortParam param, Collection<String> allowed) {
        if (param == null || !StringUtils.hasText(param.getSortField())) {
            return null;
        }
        String field = param.getSortField().trim();
        if (!allowed.contains(field)) {
            throw new BizException("framework.error.sortFieldInvalid", field);
        }
        String order = StringUtils.hasText(param.getSortOrder()) ? param.getSortOrder().trim().toLowerCase(Locale.ROOT) : ASC;
        if (!ASC.equals(order) && !DESC.equals(order)) {
            throw new BizException("framework.error.sortOrderInvalid", param.getSortOrder());
        }
        return new Sort(field, DESC.equals(order));
    }

    /** 白名单：{@code Sorts.fields("code", "name").addIf(costVisible, "amount")} */
    public static Fields fields(String... fields) {
        return new Fields().add(fields);
    }

    /** 可排序字段的集合（保持声明顺序） */
    public static final class Fields {

        private final Set<String> set = new LinkedHashSet<>();

        private Fields() {
        }

        public Fields add(String... fields) {
            set.addAll(List.of(fields));
            return this;
        }

        /** 条件成立时才可排序（如有权限才返回的金额列） */
        public Fields addIf(boolean condition, String... fields) {
            return condition ? add(fields) : this;
        }

        public Set<String> toSet() {
            return Set.copyOf(set);
        }

        /** 按白名单校验，见 {@link Sorts#resolve} */
        public Sort resolve(SortParam param) {
            return Sorts.resolve(param, set);
        }
    }
}
