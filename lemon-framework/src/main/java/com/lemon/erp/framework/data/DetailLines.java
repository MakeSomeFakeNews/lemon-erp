package com.lemon.erp.framework.data;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 单据明细行的整体替换：删除该单据原有的明细，再批量插入新的明细（单条多值 INSERT）。在调用方的事务中执行
 */
public final class DetailLines {

    private DetailLines() {
    }

    /**
     * @param parentColumn 明细表中指向单据的字段，如 {@code ScmOrderItem::getOrderId}
     * @param setParent    给明细设置单据 ID，如 {@code ScmOrderItem::setOrderId}
     * @param insertBatch  批量插入（mapper XML 中的多值 INSERT），如 {@code itemMapper::insertBatch}；明细为空时不调用
     */
    public static <T> void replace(BaseMapper<T> mapper, SFunction<T, ?> parentColumn, Long parentId, List<T> items,
                                   BiConsumer<T, Long> setParent, Consumer<List<T>> insertBatch) {
        mapper.delete(Wrappers.<T>lambdaQuery().eq(parentColumn, parentId));
        if (items == null || items.isEmpty()) {
            return;
        }
        items.forEach(i -> setParent.accept(i, parentId));
        insertBatch.accept(items);
    }
}
