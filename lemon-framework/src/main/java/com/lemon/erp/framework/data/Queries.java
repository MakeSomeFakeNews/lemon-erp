package com.lemon.erp.framework.data;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 只取前几条：用分页插件按数据库方言生成 LIMIT（不查总数），代替在 Java 中写 {@code .last("LIMIT 1")}
 */
public final class Queries {

    private Queries() {
    }

    /** 按条件（含排序）取第一条，没有返回 null */
    public static <T> T first(BaseMapper<T> mapper, Wrapper<T> wrapper) {
        List<T> list = limit(mapper, wrapper, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 按条件（含排序）取前 n 条 */
    public static <T> List<T> limit(BaseMapper<T> mapper, Wrapper<T> wrapper, int n) {
        return mapper.selectList(new Page<T>(1, n, false), wrapper);
    }
}
