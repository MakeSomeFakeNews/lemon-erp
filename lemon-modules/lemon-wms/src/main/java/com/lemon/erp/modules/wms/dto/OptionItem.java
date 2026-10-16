package com.lemon.erp.modules.wms.dto;

/**
 * 下拉选项（仓库、库位）
 */
public record OptionItem(Long id, String code, String name, String type) {

    public OptionItem(Long id, String code, String name) {
        this(id, code, name, null);
    }
}
