package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.crm.dto.CrmCustomerRequest;
import com.lemon.erp.modules.crm.entity.CrmContact;
import com.lemon.erp.modules.crm.entity.CrmCustomer;
import com.lemon.erp.modules.crm.mapper.CrmContactMapper;
import com.lemon.erp.modules.crm.mapper.CrmCustomerMapper;
import com.lemon.erp.modules.crm.model.CustomerStage;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 客户经营：客户主数据与供应链共用（scm_customer），CRM 补充负责人、行业、来源、客户阶段、跟进时间。
 * 没有负责人的客户在「公海」中，销售可以领取；主管可以分配、转移或收回到公海。
 * 「团队客户」按负责人部门 + 负责人做数据权限过滤
 */
@Service
@RequiredArgsConstructor
public class CrmCustomerService {

    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;
    private final ScmCustomerService scmCustomerService;
    private final SysUserMapper userMapper;
    private final PermissionService permissionService;

    /**
     * 我的客户 / 团队客户
     *
     * @param mine     只看我负责的
     * @param dueToday 只看今天及以前需要跟进的
     */
    @DataPermission(table = "crm_customer", deptColumn = "dept_id", userColumn = "owner_id")
    public PageResult<CrmCustomer> page(long current, long size, boolean mine, String keyword, String stage, String industry,
                                        Long ownerId, boolean dueToday) {
        Page<CrmCustomer> page = Page.of(current, size);
        customerMapper.selectOwnedPage(page, mine ? SecurityUtils.getUserId() : ownerId, keyword, stage, industry,
                dueToday ? LocalDate.now().plusDays(1).atStartOfDay() : null);
        return PageResult.of(page);
    }

    /** 公海客户 */
    public PageResult<CrmCustomer> pool(long current, long size, String keyword, String stage, String industry) {
        Page<CrmCustomer> page = Page.of(current, size);
        customerMapper.selectPoolPage(page, keyword, stage, industry);
        return PageResult.of(page);
    }

    public CrmCustomer detail(Long customerId) {
        CrmCustomer c = customerMapper.selectDetail(customerId);
        if (c == null) {
            throw new BizException("crm.error.customerDoesNotExist");
        }
        return c;
    }

    public Map<String, Object> summary(Long customerId) {
        detail(customerId);
        return customerMapper.selectSummary(customerId, LocalDate.now().minusYears(1));
    }

    /**
     * 新增客户：负责人为当前用户
     *
     * @return 客户 ID
     */
    @Transactional
    public Long create(CrmCustomerRequest req) {
        ScmCustomer sc = new ScmCustomer();
        fillScm(sc, req);
        scmCustomerService.create(sc);
        LoginUser user = SecurityUtils.getLoginUser();
        CrmCustomer x = newExt(sc.getId(), req);
        x.setOwnerId(user.userId());
        x.setDeptId(user.deptId());
        x.setClaimTime(LocalDateTime.now());
        customerMapper.insert(x);
        if (StringUtils.hasText(req.contact())) {
            CrmContact contact = new CrmContact();
            contact.setCustomerId(sc.getId());
            contact.setName(req.contact().trim());
            contact.setPhone(req.phone());
            contact.setIsPrimary(1);
            contactMapper.insert(contact);
        }
        return sc.getId();
    }

    /** 修改客户基本信息与经营信息：负责人本人或有修改权限 */
    @Transactional
    public void update(Long customerId, CrmCustomerRequest req) {
        CrmCustomer c = detail(customerId);
        checkOwnerOrPerm(c, "crm:customer:edit");
        ScmCustomer sc = scmCustomerService.get(customerId);
        fillScm(sc, req);
        scmCustomerService.update(customerId, sc);
        CrmCustomer x = ensure(customerId);
        CrmCustomer ext = newExt(customerId, req);
        customerMapper.update(Wrappers.<CrmCustomer>lambdaUpdate()
                .set(CrmCustomer::getIndustry, ext.getIndustry())
                .set(CrmCustomer::getSource, ext.getSource())
                .set(CrmCustomer::getStage, ext.getStage())
                .set(CrmCustomer::getRegion, ext.getRegion())
                .set(CrmCustomer::getWebsite, ext.getWebsite())
                .set(CrmCustomer::getUpdateTime, LocalDateTime.now())
                .eq(CrmCustomer::getId, x.getId()));
    }

    /** 分配 / 转移客户，ownerId 为空放回公海 */
    @Transactional
    public void assign(List<Long> customerIds, Long ownerId) {
        Long deptId = null;
        if (ownerId != null) {
            SysUser owner = userMapper.selectById(ownerId);
            if (owner == null || !Integer.valueOf(1).equals(owner.getStatus())) {
                throw new BizException("crm.error.ownerDoesNotExistDisabled");
            }
            deptId = owner.getDeptId();
        }
        for (Long id : customerIds) {
            detail(id);
            CrmCustomer x = ensure(id);
            customerMapper.update(Wrappers.<CrmCustomer>lambdaUpdate()
                    .set(CrmCustomer::getOwnerId, ownerId)
                    .set(CrmCustomer::getDeptId, deptId)
                    .set(CrmCustomer::getClaimTime, ownerId == null ? null : LocalDateTime.now())
                    .set(CrmCustomer::getUpdateTime, LocalDateTime.now())
                    .eq(CrmCustomer::getId, x.getId()));
        }
    }

    /**
     * 领取公海客户
     *
     * @return 领取成功的数量（已被他人领取的跳过）
     */
    @Transactional
    public int claim(List<Long> customerIds) {
        LoginUser user = SecurityUtils.getLoginUser();
        int count = 0;
        for (Long id : customerIds) {
            detail(id);
            ensure(id);
            count += customerMapper.claim(id, user.userId(), user.deptId());
        }
        return count;
    }

    /** 放回公海：负责人本人或有分配权限 */
    @Transactional
    public void release(List<Long> customerIds) {
        for (Long id : customerIds) {
            checkOwnerOrPerm(detail(id), "crm:customer:assign");
        }
        assign(customerIds, null);
    }

    // ---------------------------------------------------------------- 联系人

    public List<CrmContact> contacts(Long customerId) {
        return contactMapper.selectList(Wrappers.<CrmContact>lambdaQuery()
                .eq(CrmContact::getCustomerId, customerId)
                .orderByDesc(CrmContact::getIsPrimary).orderByAsc(CrmContact::getId));
    }

    @Transactional
    public void saveContact(Long id, CrmContact contact) {
        CrmCustomer c = detail(contact.getCustomerId());
        checkOwnerOrPerm(c, "crm:customer:edit");
        boolean primary = Integer.valueOf(1).equals(contact.getIsPrimary())
                || !contactMapper.exists(Wrappers.<CrmContact>lambdaQuery().eq(CrmContact::getCustomerId, contact.getCustomerId()));
        contact.setIsPrimary(primary ? 1 : 0);
        if (id == null) {
            contact.setId(null);
            contactMapper.insert(contact);
        } else {
            CrmContact old = contactMapper.selectById(id);
            if (old == null || !Objects.equals(old.getCustomerId(), contact.getCustomerId())) {
                throw new BizException("crm.error.contactDoesNotExist");
            }
            contact.setId(id);
            contactMapper.updateById(contact);
        }
        if (primary) {
            contactMapper.update(Wrappers.<CrmContact>lambdaUpdate().set(CrmContact::getIsPrimary, 0)
                    .eq(CrmContact::getCustomerId, contact.getCustomerId()).ne(CrmContact::getId, contact.getId()));
        }
    }

    @Transactional
    public void deleteContact(Long id) {
        CrmContact contact = contactMapper.selectById(id);
        if (contact == null) {
            return;
        }
        checkOwnerOrPerm(detail(contact.getCustomerId()), "crm:customer:edit");
        contactMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 供其他服务使用

    /** 客户的经营信息，没有时新建（在公海） */
    CrmCustomer ensure(Long customerId) {
        CrmCustomer x = customerMapper.selectOne(Wrappers.<CrmCustomer>lambdaQuery().eq(CrmCustomer::getCustomerId, customerId));
        if (x != null) {
            return x;
        }
        x = new CrmCustomer();
        x.setCustomerId(customerId);
        x.setStage(CustomerStage.POTENTIAL);
        try {
            customerMapper.insert(x);
        } catch (DuplicateKeyException e) {
            return customerMapper.selectOne(Wrappers.<CrmCustomer>lambdaQuery().eq(CrmCustomer::getCustomerId, customerId));
        }
        return x;
    }

    /** 新建线索转化来的客户 */
    @Transactional
    public Long createFromLead(ScmCustomer sc, CrmCustomer ext, CrmContact contact) {
        scmCustomerService.create(sc);
        ext.setCustomerId(sc.getId());
        customerMapper.insert(ext);
        if (contact != null) {
            contact.setCustomerId(sc.getId());
            contact.setIsPrimary(1);
            contactMapper.insert(contact);
        }
        return sc.getId();
    }

    /** 写跟进后更新最近 / 下次跟进时间 */
    void touchFollow(Long customerId, LocalDateTime followTime, LocalDateTime nextTime) {
        CrmCustomer x = ensure(customerId);
        customerMapper.update(Wrappers.<CrmCustomer>lambdaUpdate()
                .set(CrmCustomer::getLastFollowTime, followTime)
                .set(CrmCustomer::getNextFollowTime, nextTime)
                .eq(CrmCustomer::getId, x.getId())
                .and(w -> w.isNull(CrmCustomer::getLastFollowTime).or().le(CrmCustomer::getLastFollowTime, followTime)));
    }

    /** 赢单：客户阶段变为成交 */
    void markDeal(Long customerId) {
        CrmCustomer x = ensure(customerId);
        customerMapper.update(Wrappers.<CrmCustomer>lambdaUpdate().set(CrmCustomer::getStage, CustomerStage.DEAL).eq(CrmCustomer::getId, x.getId()));
    }

    void checkOwnerOrPerm(CrmCustomer c, String perm) {
        if (!Objects.equals(c.getOwnerId(), SecurityUtils.getUserId()) && !permissionService.hasPerm(perm)) {
            throw new BizException("crm.error.onlyOwnerOperateCustomer");
        }
    }

    private static void fillScm(ScmCustomer sc, CrmCustomerRequest req) {
        sc.setCode(req.code());
        sc.setName(req.name().trim());
        sc.setLevel(req.level());
        sc.setContact(req.contact());
        sc.setPhone(req.phone());
        sc.setAddress(req.address());
    }

    private static CrmCustomer newExt(Long customerId, CrmCustomerRequest req) {
        CrmCustomer x = new CrmCustomer();
        x.setCustomerId(customerId);
        x.setIndustry(req.industry());
        x.setSource(req.source());
        x.setStage(req.stage() == null ? CustomerStage.POTENTIAL : req.stage());
        x.setRegion(req.region());
        x.setWebsite(req.website());
        return x;
    }
}
