package com.lemon.erp.modules.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.workflow.entity.WfCopy;
import com.lemon.erp.modules.workflow.mapper.WfCopyMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 抄送记录
 */
@Service
@RequiredArgsConstructor
public class WfCopyService {

    private final WfCopyMapper copyMapper;
    private final WfInstanceLookup instanceLookup;
    private final FlowIdentityProvider identityProvider;

    /**
     * 记录抄送
     *
     * @param copy    流程、节点、发起人等公共信息
     * @param userIds 被抄送人
     */
    public void create(WfCopy copy, Collection<Long> userIds) {
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            // 与自动填充一致使用应用服务器时间，不用数据库的 NOW()
            copy.setCreateTime(LocalDateTime.now());
            copyMapper.insertBatch(copy, distinct);
        }
    }

    /** 抄送给当前用户的流程 */
    public PageResult<WfCopy> pageMine(String name, long current, long size) {
        Page<WfCopy> page = copyMapper.selectPage(Page.of(current, size), Wrappers.<WfCopy>lambdaQuery()
                .eq(WfCopy::getUserId, SecurityUtils.getUserId())
                .like(StringUtils.hasText(name), WfCopy::getProcessName, name)
                .orderByDesc(WfCopy::getId));
        List<WfCopy> records = page.getRecords();
        Map<String, HistoricProcessInstance> instances = instanceLookup.instances(
                records.stream().map(WfCopy::getProcessInstanceId).collect(Collectors.toSet()));
        Map<Long, String> names = identityProvider.getUserNames(
                records.stream().map(WfCopy::getStartUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        records.forEach(c -> {
            HistoricProcessInstance instance = instances.get(c.getProcessInstanceId());
            c.setStatus(instance == null ? null : instance.getBusinessStatus());
            c.setStartUserName(names.get(c.getStartUserId()));
        });
        return PageResult.of(page);
    }

    public List<WfCopy> listByInstance(String processInstanceId) {
        return copyMapper.selectList(Wrappers.<WfCopy>lambdaQuery()
                .eq(WfCopy::getProcessInstanceId, processInstanceId)
                .orderByAsc(WfCopy::getId));
    }

    public boolean isCopiedTo(String processInstanceId, Long userId) {
        return copyMapper.exists(Wrappers.<WfCopy>lambdaQuery()
                .eq(WfCopy::getProcessInstanceId, processInstanceId)
                .eq(WfCopy::getUserId, userId));
    }
}
