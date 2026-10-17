package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.crm.dto.FollowupRequest;
import com.lemon.erp.modules.crm.entity.CrmFollowup;
import com.lemon.erp.modules.crm.entity.CrmOpportunity;
import com.lemon.erp.modules.crm.mapper.CrmFollowupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 跟进记录：跟进客户、线索或商机，更新对象的最近 / 下次跟进时间；商机的跟进同时计入所属客户
 */
@Service
@RequiredArgsConstructor
public class CrmFollowupService {

    private final CrmFollowupMapper followupMapper;
    private final CrmCustomerService customerService;
    private final CrmLeadService leadService;
    private final CrmOpportunityService opportunityService;
    private final PermissionService permissionService;

    @DataPermission(table = "crm_followup", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<CrmFollowup> page(long current, long size, String targetType, Long targetId, Long customerId,
                                        boolean mine, String method, LocalDate beginDate, LocalDate endDate) {
        Page<CrmFollowup> page = Page.of(current, size);
        followupMapper.selectPageList(page, targetType, targetId, customerId, mine ? SecurityUtils.getUserId() : null, method,
                beginDate == null ? null : beginDate.atStartOfDay(), endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        return PageResult.of(page);
    }

    @Transactional
    public Long create(FollowupRequest req) {
        LocalDateTime time = req.followTime() != null ? req.followTime() : LocalDateTime.now().withNano(0);
        if (req.nextTime() != null && !req.nextTime().isAfter(time)) {
            throw new BizException("crm.error.nextFollowUpTimeMustLater");
        }
        Long customerId = null;
        switch (req.targetType()) {
            case CUSTOMER -> {
                customerService.detail(req.targetId());
                customerId = req.targetId();
                customerService.touchFollow(customerId, time, req.nextTime());
            }
            case LEAD -> leadService.touchFollow(req.targetId(), time);
            case OPPORTUNITY -> {
                CrmOpportunity o = opportunityService.get(req.targetId());
                customerId = o.getCustomerId();
                customerService.touchFollow(customerId, time, req.nextTime());
            }
        }
        LoginUser user = SecurityUtils.getLoginUser();
        CrmFollowup f = new CrmFollowup();
        f.setTargetType(req.targetType());
        f.setTargetId(req.targetId());
        f.setCustomerId(customerId);
        f.setMethod(req.method());
        f.setContent(req.content().trim());
        f.setFollowTime(time);
        f.setNextTime(req.nextTime());
        f.setNextPlan(req.nextPlan());
        f.setDeptId(user.deptId());
        followupMapper.insert(f);
        return f.getId();
    }

    /** 删除：本人的跟进，或有删除权限 */
    @Transactional
    public void delete(Long id) {
        CrmFollowup f = followupMapper.selectById(id);
        if (f == null) {
            return;
        }
        if (!Objects.equals(f.getCreateBy(), SecurityUtils.getUserId()) && !permissionService.hasPerm("crm:followup:remove")) {
            throw new BizException("crm.error.onlyDeleteOwnFollowUps");
        }
        followupMapper.deleteById(id);
    }
}
