package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.crm.entity.CrmCampaign;
import com.lemon.erp.modules.crm.entity.CrmLead;
import com.lemon.erp.modules.crm.entity.CrmOpportunity;
import com.lemon.erp.modules.crm.mapper.CrmCampaignMapper;
import com.lemon.erp.modules.crm.mapper.CrmLeadMapper;
import com.lemon.erp.modules.crm.mapper.CrmOpportunityMapper;
import com.lemon.erp.modules.crm.model.CampaignStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 市场活动：效果按关联的线索、商机统计，投入产出比 = 赢单金额 / 实际花费
 */
@Service
@RequiredArgsConstructor
public class CrmCampaignService {

    private final CrmCampaignMapper campaignMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmOpportunityMapper opportunityMapper;

    public PageResult<CrmCampaign> page(long current, long size, String keyword, String type, String status) {
        Page<CrmCampaign> page = Page.of(current, size);
        campaignMapper.selectPageList(page, keyword, type, status);
        return PageResult.of(page);
    }

    public CrmCampaign detail(Long id) {
        CrmCampaign c = campaignMapper.selectDetail(id);
        if (c == null) {
            throw new BizException("crm.error.campaignDoesNotExist");
        }
        return c;
    }

    /** 线索、商机选择来源活动：未取消的活动 */
    public List<CrmCampaign> options() {
        return campaignMapper.selectList(Wrappers.<CrmCampaign>lambdaQuery()
                .select(CrmCampaign::getId, CrmCampaign::getName, CrmCampaign::getType, CrmCampaign::getStatus)
                .ne(CrmCampaign::getStatus, CampaignStatus.CANCELED)
                .orderByDesc(CrmCampaign::getStartDate));
    }

    public void create(CrmCampaign c) {
        check(c);
        c.setId(null);
        if (c.getStatus() == null) c.setStatus(CampaignStatus.PLANNED);
        campaignMapper.insert(c);
    }

    public void update(Long id, CrmCampaign c) {
        detail(id);
        check(c);
        c.setId(id);
        campaignMapper.updateById(c);
    }

    /** 已有线索或商机关联的活动不能删除，可以取消 */
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            if (leadMapper.exists(Wrappers.<CrmLead>lambdaQuery().eq(CrmLead::getCampaignId, id))
                    || opportunityMapper.exists(Wrappers.<CrmOpportunity>lambdaQuery().eq(CrmOpportunity::getCampaignId, id))) {
                throw new BizException("crm.error.campaignAlreadyHasLeadsOpportunitiesCannot", detail(id).getName());
            }
        }
        campaignMapper.deleteByIds(ids);
    }

    private static void check(CrmCampaign c) {
        if (c.getEndDate().isBefore(c.getStartDate())) {
            throw new BizException("crm.error.endDateCannotEarlierStartDate");
        }
        c.setBudget(Decimals.nz(c.getBudget()));
        c.setActualCost(Decimals.nz(c.getActualCost()));
    }
}
