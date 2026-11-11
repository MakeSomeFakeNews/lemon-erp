package com.lemon.erp.framework.data;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lemon.erp.framework.tenant.TenantContext;

import java.util.List;
import java.util.Objects;

/**
 * 有检索列的表：业务模块声明为 Bean（如 {@code WmsSearchKeys}），供修改后刷新检索列和启动时回填（{@link SearchKeyBackfill}）
 *
 * @param name      表名，只用于日志
 * @param mapper    本模块的 Mapper
 * @param idColumn  主键
 * @param keyColumn 检索列
 */
public record SearchKeyTable<T extends Searchable>(String name, BaseMapper<T> mapper, SFunction<T, Long> idColumn,
                                                   SFunction<T, String> keyColumn) {

    /** 每批回填的行数 */
    static final int BATCH = 500;

    /**
     * 修改记录后重新计算检索列，有变化时更新
     *
     * @param row 修改后从数据库重新读取的记录（修改接口可能只更新部分字段，不能用请求中的实体），为空时忽略
     */
    public void refresh(T row) {
        if (row != null) {
            update(row);
        }
    }

    /**
     * 回填检索列为 NULL 的行（全部租户），按主键分批，可重复执行
     *
     * @return 回填的行数
     */
    public int backfill() {
        return TenantContext.callIgnore(() -> {
            int count = 0;
            long lastId = 0;
            while (true) {
                List<T> rows = Queries.limit(mapper, Wrappers.<T>lambdaQuery()
                        .isNull(keyColumn).gt(idColumn, lastId).orderByAsc(idColumn), BATCH);
                for (T row : rows) {
                    update(row);
                    lastId = idColumn.apply(row);
                }
                count += rows.size();
                if (rows.size() < BATCH) {
                    return count;
                }
            }
        });
    }

    private void update(T row) {
        String key = SearchKeys.of(row);
        if (!Objects.equals(key, row.getSearchKey())) {
            mapper.update(Wrappers.<T>lambdaUpdate().set(keyColumn, key).eq(idColumn, idColumn.apply(row)));
            row.setSearchKey(key);
        }
    }
}
