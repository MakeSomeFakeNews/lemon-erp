package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.framework.config.MybatisPlusConfig;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 列表展示时批量补充流程实例信息，以及引擎分页、数据类型的转换
 */
@Component
@RequiredArgsConstructor
public class WfInstanceLookup {

    private final HistoryService historyService;

    /** 批量查询流程实例（含已结束的），一次查询 */
    public Map<String, HistoricProcessInstance> instances(Collection<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(new HashSet<>(processInstanceIds)).list()
                .stream().collect(Collectors.toMap(HistoricProcessInstance::getId, Function.identity()));
    }

    /** 引擎分页查询的起始行（current 从 1 开始） */
    public static int firstResult(long current, long size) {
        return (int) ((Math.max(current, 1) - 1) * maxResults(size));
    }

    /** 引擎分页查询的每页条数，与 MyBatis-Plus 分页一样限制上限 */
    public static int maxResults(long size) {
        return (int) Math.min(Math.max(size, 1), MybatisPlusConfig.MAX_PAGE_SIZE);
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /** 引擎中的用户 ID 是字符串 */
    public static Long toUserId(String userId) {
        return userId == null ? null : Long.valueOf(userId);
    }
}
