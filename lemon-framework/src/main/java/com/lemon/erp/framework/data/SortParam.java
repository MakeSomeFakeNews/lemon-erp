package com.lemon.erp.framework.data;

import lombok.Data;

/**
 * 列表 / 报表的列头排序参数（GET：sortField、sortOrder = asc | desc，issue #122），与查询条件一起绑定。
 * 不能直接用于 SQL：由 Service 用 {@link Sorts#resolve} 按白名单校验为 {@link Sort} 后传给 mapper
 */
@Data
public class SortParam {

    /** 前端的列字段名（与返回的字段名一致），为空时按默认顺序 */
    private String sortField;
    /** asc 升序 / desc 降序，为空时升序 */
    private String sortOrder;

    public SortParam() {
    }

    public SortParam(String sortField, String sortOrder) {
        this.sortField = sortField;
        this.sortOrder = sortOrder;
    }
}
