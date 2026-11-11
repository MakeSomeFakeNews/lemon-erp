package com.lemon.erp.framework.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动后回填检索列为 NULL 的行：升级前已有的数据、init.sql 的初始数据、直接写库新增的记录。
 * 在应用就绪事件中执行（此时已能处理请求，回填期间未回填的行仍可按编码、名称检索），只处理 NULL 行，可重复执行；
 * 多实例同时回填写入的值相同，不冲突。一张表失败只记录日志，不影响启动和其他表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchKeyBackfill {

    private final List<SearchKeyTable<?>> tables;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        for (SearchKeyTable<?> table : tables) {
            try {
                int count = table.backfill();
                if (count > 0) {
                    log.info("Backfilled search_key of {} rows in {}", count, table.name());
                }
            } catch (RuntimeException e) {
                log.warn("Failed to backfill search_key in {}", table.name(), e);
            }
        }
    }
}
