package com.lemon.erp.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResult<T>(List<T> records, long total) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
}
