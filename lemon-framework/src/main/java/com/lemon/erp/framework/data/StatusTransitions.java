package com.lemon.erp.framework.data;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lemon.erp.common.exception.BizException;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * 单据状态流转：按原状态条件更新（UPDATE ... SET status = 目标 WHERE id = ? AND status = 原状态），
 * 并发修改时只有一个请求成功，另一个更新 0 行并提示刷新。idColumn 一般为主键，也可以是其他唯一字段（如会计期间）
 */
public final class StatusTransitions {

    private StatusTransitions() {
    }

    /**
     * 状态从 from 改为 to，失败（状态已被他人改变）时抛出 BizException(errorKey, args)
     */
    public static <T, S> void change(BaseMapper<T> mapper, SFunction<T, ?> idColumn, Object id,
                                     SFunction<T, ?> statusColumn, S from, S to, String errorKey, Object... args) {
        if (!tryChange(mapper, idColumn, id, statusColumn, from, to, null)) {
            throw new BizException(errorKey, args);
        }
    }

    /**
     * 状态从 from 改为 to，同时设置其他字段（extra 中追加 set，如确认人、确认时间），失败时抛出 BizException(errorKey, args)
     */
    public static <T, S> void change(BaseMapper<T> mapper, SFunction<T, ?> idColumn, Object id,
                                     SFunction<T, ?> statusColumn, S from, S to,
                                     Consumer<LambdaUpdateWrapper<T>> extra, String errorKey, Object... args) {
        if (!tryChange(mapper, idColumn, id, statusColumn, from, to, extra)) {
            throw new BizException(errorKey, args);
        }
    }

    /** 状态从 from 改为 to，返回是否更新成功 */
    public static <T, S> boolean tryChange(BaseMapper<T> mapper, SFunction<T, ?> idColumn, Object id,
                                           SFunction<T, ?> statusColumn, S from, S to) {
        return tryChange(mapper, idColumn, id, statusColumn, from, to, null);
    }

    /**
     * 状态从 from 改为 to，同时设置其他字段（extra 中追加 set）；返回是否更新成功
     */
    public static <T, S> boolean tryChange(BaseMapper<T> mapper, SFunction<T, ?> idColumn, Object id,
                                           SFunction<T, ?> statusColumn, S from, S to,
                                           Consumer<LambdaUpdateWrapper<T>> extra) {
        LambdaUpdateWrapper<T> w = Wrappers.<T>lambdaUpdate().set(statusColumn, to).eq(idColumn, id).eq(statusColumn, from);
        if (extra != null) {
            extra.accept(w);
        }
        return mapper.update(w) > 0;
    }

    /** 状态从 from 中任一个改为 to，失败时抛出 BizException(errorKey, args) */
    public static <T, S> void changeFromAny(BaseMapper<T> mapper, SFunction<T, ?> idColumn, Object id,
                                            SFunction<T, ?> statusColumn, Collection<S> from, S to,
                                            String errorKey, Object... args) {
        if (!tryChangeFromAny(mapper, idColumn, id, statusColumn, from, to, null)) {
            throw new BizException(errorKey, args);
        }
    }

    /** 状态从 from 中任一个改为 to，同时设置其他字段（extra 可为 null）；返回是否更新成功 */
    public static <T, S> boolean tryChangeFromAny(BaseMapper<T> mapper, SFunction<T, ?> idColumn, Object id,
                                                  SFunction<T, ?> statusColumn, Collection<S> from, S to,
                                                  Consumer<LambdaUpdateWrapper<T>> extra) {
        LambdaUpdateWrapper<T> w = Wrappers.<T>lambdaUpdate().set(statusColumn, to).eq(idColumn, id).in(statusColumn, from);
        if (extra != null) {
            extra.accept(w);
        }
        return mapper.update(w) > 0;
    }
}
