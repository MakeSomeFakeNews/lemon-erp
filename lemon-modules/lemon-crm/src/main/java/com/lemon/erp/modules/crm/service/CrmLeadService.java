package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.crm.dto.LeadConvertRequest;
import com.lemon.erp.modules.crm.dto.LeadRequest;
import com.lemon.erp.modules.crm.dto.OpportunityRequest;
import com.lemon.erp.modules.crm.entity.CrmContact;
import com.lemon.erp.modules.crm.entity.CrmCustomer;
import com.lemon.erp.modules.crm.entity.CrmLead;
import com.lemon.erp.modules.crm.mapper.CrmLeadMapper;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 销售线索：待跟进 → 跟进中（写过跟进）→ 已转化（生成客户、联系人，可同时生成商机）/ 无效。
 * 数据权限按负责人部门 + 负责人过滤
 */
@Service
@RequiredArgsConstructor
public class CrmLeadService {

    private static final Set<String> OPEN = Set.of("NEW", "FOLLOWING");

    private final CrmLeadMapper leadMapper;
    private final CrmCustomerService customerService;
    private final CrmOpportunityService opportunityService;
    private final SysUserMapper userMapper;
    private final DocNoService docNoService;

    @DataPermission(table = "crm_lead", deptColumn = "dept_id", userColumn = "owner_id")
    public PageResult<CrmLead> page(long current, long size, String keyword, String status, String source, Long campaignId, Long ownerId) {
        Page<CrmLead> page = Page.of(current, size);
        leadMapper.selectPageList(page, keyword, status, source, campaignId, ownerId);
        return PageResult.of(page);
    }

    public CrmLead detail(Long id) {
        CrmLead l = leadMapper.selectDetail(id);
        if (l == null) {
            throw new BizException("crm.error.leadDoesNotExist");
        }
        return l;
    }

    @Transactional
    public Long create(LeadRequest req) {
        CrmLead l = new CrmLead();
        fill(l, req);
        l.setLeadNo(docNoService.next("XL"));
        l.setStatus("NEW");
        l.setCreateBy(SecurityUtils.getUserId());
        leadMapper.insert(l);
        return l.getId();
    }

    @Transactional
    public void update(Long id, LeadRequest req) {
        CrmLead old = getOpen(id);
        CrmLead l = new CrmLead();
        fill(l, req);
        leadMapper.update(Wrappers.<CrmLead>lambdaUpdate()
                .set(CrmLead::getCompany, l.getCompany())
                .set(CrmLead::getContactName, l.getContactName())
                .set(CrmLead::getTitle, l.getTitle())
                .set(CrmLead::getPhone, l.getPhone())
                .set(CrmLead::getEmail, l.getEmail())
                .set(CrmLead::getIndustry, l.getIndustry())
                .set(CrmLead::getSource, l.getSource())
                .set(CrmLead::getCampaignId, l.getCampaignId())
                .set(CrmLead::getRegion, l.getRegion())
                .set(CrmLead::getRequirement, l.getRequirement())
                .set(CrmLead::getOwnerId, l.getOwnerId())
                .set(CrmLead::getDeptId, l.getDeptId())
                .set(CrmLead::getUpdateTime, LocalDateTime.now())
                .eq(CrmLead::getId, old.getId()));
    }

    /** 已转化的线索不能删除 */
    @Transactional
    public void delete(List<Long> ids) {
        for (CrmLead l : leadMapper.selectByIds(ids)) {
            if ("CONVERTED".equals(l.getStatus())) {
                throw new BizException("crm.error.leadHasConvertedCustomerCannotDeleted", l.getCompany());
            }
        }
        leadMapper.deleteByIds(ids);
    }

    @Transactional
    public void invalidate(Long id, String reason) {
        getOpen(id);
        leadMapper.update(Wrappers.<CrmLead>lambdaUpdate()
                .set(CrmLead::getStatus, "INVALID").set(CrmLead::getInvalidReason, reason)
                .eq(CrmLead::getId, id));
    }

    /**
     * 转化为客户：负责人沿用线索负责人，客户阶段为意向客户；可同时生成商机
     *
     * @return { customerId, opportunityId }
     */
    @Transactional
    public Map<String, Long> convert(Long id, LeadConvertRequest req) {
        CrmLead l = getOpen(id);
        ScmCustomer sc = new ScmCustomer();
        sc.setCode(req.customerCode());
        sc.setName(l.getCompany());
        sc.setContact(l.getContactName());
        sc.setPhone(l.getPhone());
        sc.setLevel("POTENTIAL");
        CrmCustomer ext = new CrmCustomer();
        ext.setOwnerId(l.getOwnerId());
        ext.setDeptId(l.getDeptId());
        ext.setIndustry(l.getIndustry());
        ext.setSource(l.getSource());
        ext.setRegion(l.getRegion());
        ext.setStage("INTENT");
        ext.setLastFollowTime(l.getLastFollowTime());
        ext.setClaimTime(l.getOwnerId() == null ? null : LocalDateTime.now());
        CrmContact contact = null;
        if (StringUtils.hasText(l.getContactName())) {
            contact = new CrmContact();
            contact.setName(l.getContactName());
            contact.setTitle(l.getTitle());
            contact.setPhone(l.getPhone());
            contact.setEmail(l.getEmail());
        }
        Long customerId = customerService.createFromLead(sc, ext, contact);
        leadMapper.update(Wrappers.<CrmLead>lambdaUpdate()
                .set(CrmLead::getStatus, "CONVERTED")
                .set(CrmLead::getCustomerId, customerId)
                .set(CrmLead::getConvertTime, LocalDateTime.now())
                .eq(CrmLead::getId, id));
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("customerId", customerId);
        if (Boolean.TRUE.equals(req.createOpportunity())) {
            String name = StringUtils.hasText(req.opportunityName()) ? req.opportunityName().trim() : l.getCompany() + " 商机"; // i18n-ignore 存库的默认商机名称
            Long oppId = opportunityService.create(new OpportunityRequest(name, customerId, null, req.amount(), null, null,
                    req.expectedDate(), l.getSource(), l.getCampaignId(), l.getOwnerId(), l.getRequirement(), null), id);
            result.put("opportunityId", oppId);
        }
        return result;
    }

    /** 写跟进：更新最近跟进时间，待跟进变为跟进中 */
    void touchFollow(Long id, LocalDateTime followTime) {
        CrmLead l = detail(id);
        leadMapper.update(Wrappers.<CrmLead>lambdaUpdate()
                .set(CrmLead::getLastFollowTime, followTime)
                .set("NEW".equals(l.getStatus()), CrmLead::getStatus, "FOLLOWING")
                .eq(CrmLead::getId, id));
    }

    private CrmLead getOpen(Long id) {
        CrmLead l = detail(id);
        if (!OPEN.contains(l.getStatus())) {
            throw new BizException("crm.error.leadHasAlreadyConvertedVoided");
        }
        return l;
    }

    private void fill(CrmLead l, LeadRequest req) {
        l.setCompany(req.company().trim());
        l.setContactName(req.contactName());
        l.setTitle(req.title());
        l.setPhone(req.phone());
        l.setEmail(req.email());
        l.setIndustry(req.industry());
        l.setSource(req.source());
        l.setCampaignId(req.campaignId());
        l.setRegion(req.region());
        l.setRequirement(req.requirement());
        if (req.ownerId() == null) {
            LoginUser user = SecurityUtils.getLoginUser();
            l.setOwnerId(user.userId());
            l.setDeptId(user.deptId());
        } else {
            SysUser owner = userMapper.selectById(req.ownerId());
            if (owner == null) {
                throw new BizException("crm.error.ownerDoesNotExist");
            }
            l.setOwnerId(owner.getId());
            l.setDeptId(owner.getDeptId());
        }
    }
}
