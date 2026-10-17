package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.crm.dto.ContractRequest;
import com.lemon.erp.modules.crm.entity.CrmCollectionPlan;
import com.lemon.erp.modules.crm.entity.CrmContract;
import com.lemon.erp.modules.crm.entity.CrmOpportunity;
import com.lemon.erp.modules.crm.mapper.CrmCollectionPlanMapper;
import com.lemon.erp.modules.crm.mapper.CrmContractMapper;
import com.lemon.erp.modules.crm.mapper.CrmOpportunityMapper;
import com.lemon.erp.modules.crm.model.CollectStatus;
import com.lemon.erp.modules.crm.model.SalesContractStatus;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmQuotation;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmQuotationMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmCustomerService;
import com.lemon.erp.modules.scm.service.ScmQuotationService;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 销售合同：可来自商机、报价单；关联销售订单（选已有订单，或由已批准的报价单生成），回款 = 该订单销售出库应收单的核销金额，
 * 按回款计划的日期先后分摊，得出各期回款状态（已回款 / 部分回款 / 逾期 / 未到期）
 */
@Service
@RequiredArgsConstructor
public class CrmContractService {

    public static final String PROCESS_KEY = "crm_contract";
    private static final String BUSINESS_KEY_PREFIX = "crm_contract:";
    private static final DataPermissionRule RULE = new DataPermissionRule("crm_contract", "dept_id", "owner_id");

    private final CrmContractMapper contractMapper;
    private final CrmCollectionPlanMapper planMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final ScmCustomerService customerService;
    private final ScmQuotationMapper quotationMapper;
    private final ScmQuotationService quotationService;
    private final ScmOrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final DocNoService docNoService;
    private final FlowApi flowApi;

    @DataPermission(table = "crm_contract", deptColumn = "dept_id", userColumn = "owner_id")
    public PageResult<CrmContract> page(long current, long size, String keyword, String status, Long customerId, Long opportunityId) {
        Page<CrmContract> page = Page.of(current, size);
        contractMapper.selectPageList(page, keyword, status, customerId, opportunityId);
        return PageResult.of(page);
    }

    public CrmContract detail(Long id) {
        getAccessible(id);
        CrmContract c = contractMapper.selectDetail(id);
        c.setPlans(plans(c));
        return c;
    }

    @Transactional
    public Long create(ContractRequest req) {
        CrmContract c = new CrmContract();
        c.setContractNo(docNoService.next(CrmDocNos.CONTRACT));
        c.setStatus(SalesContractStatus.DRAFT);
        c.setCreateBy(SecurityUtils.getUserId());
        fill(c, req);
        contractMapper.insert(c);
        savePlans(c.getId(), req);
        return c.getId();
    }

    @Transactional
    public void update(Long id, ContractRequest req) {
        requireStatus(id, SalesContractStatus.DRAFT);
        CrmContract c = new CrmContract();
        fill(c, req);
        contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getName, c.getName())
                .set(CrmContract::getCustomerId, c.getCustomerId()).set(CrmContract::getOpportunityId, c.getOpportunityId())
                .set(CrmContract::getQuotationId, c.getQuotationId()).set(CrmContract::getSignDate, c.getSignDate())
                .set(CrmContract::getStartDate, c.getStartDate()).set(CrmContract::getEndDate, c.getEndDate())
                .set(CrmContract::getAmount, c.getAmount()).set(CrmContract::getOwnerId, c.getOwnerId())
                .set(CrmContract::getDeptId, c.getDeptId()).set(CrmContract::getRemark, c.getRemark())
                .set(CrmContract::getUpdateTime, LocalDateTime.now()).eq(CrmContract::getId, id));
        planMapper.delete(Wrappers.<CrmCollectionPlan>lambdaQuery().eq(CrmCollectionPlan::getContractId, id));
        savePlans(id, req);
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, SalesContractStatus.DRAFT);
        planMapper.delete(Wrappers.<CrmCollectionPlan>lambdaQuery().eq(CrmCollectionPlan::getContractId, id));
        contractMapper.deleteById(id);
    }

    /** @return true 已发起审批 */
    @Transactional
    public boolean submit(Long id) {
        CrmContract c = requireStatus(id, SalesContractStatus.DRAFT);
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            activate(id, SalesContractStatus.DRAFT);
            return false;
        }
        transit(id, SalesContractStatus.DRAFT, SalesContractStatus.APPROVING);
        String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id,
                Map.of("contractNo", c.getContractNo(), "amount", c.getAmount(), "customerId", c.getCustomerId()));
        contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getFlowInstanceId, instanceId).eq(CrmContract::getId, id));
        return true;
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        if (e.status() == FlowStatus.APPROVED) {
            activate(id, SalesContractStatus.APPROVING);
        } else {
            contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getStatus, SalesContractStatus.DRAFT)
                    .eq(CrmContract::getId, id).eq(CrmContract::getStatus, SalesContractStatus.APPROVING));
        }
    }

    /** 关联已有的销售订单（同一客户、已审核及之后） */
    @Transactional
    public void linkOrder(Long id, Long orderId) {
        CrmContract c = requireStatus(id, SalesContractStatus.ACTIVE);
        ScmOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getKind() != OrderKind.SALES || !order.getPartnerId().equals(c.getCustomerId())) {
            throw new BizException("crm.error.selectSalesOrderCustomer");
        }
        if (order.getStatus().isUnaudited()) {
            throw new BizException("crm.error.salesOrderOnlyLinkedContractAfter");
        }
        if (contractMapper.exists(Wrappers.<CrmContract>lambdaQuery().eq(CrmContract::getOrderId, orderId).ne(CrmContract::getId, id))) {
            throw new BizException("crm.error.salesOrderAlreadyLinkedAnotherContract");
        }
        contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getOrderId, orderId).eq(CrmContract::getId, id));
    }

    /** 由合同关联的已批准报价单生成销售订单（草稿）并关联 */
    @Transactional
    public Long toOrder(Long id, Long warehouseId) {
        CrmContract c = requireStatus(id, SalesContractStatus.ACTIVE);
        if (c.getOrderId() != null) {
            throw new BizException("crm.error.contractAlreadyLinkedSalesOrder", c.getOrderNo());
        }
        if (c.getQuotationId() == null) {
            throw new BizException("crm.error.contractHasNoLinkedQuotationLink");
        }
        Long orderId = quotationService.toOrder(c.getQuotationId(), warehouseId, c.getStartDate());
        contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getOrderId, orderId).eq(CrmContract::getId, id));
        return orderId;
    }

    @Transactional
    public void complete(Long id) {
        requireStatus(id, SalesContractStatus.ACTIVE);
        transit(id, SalesContractStatus.ACTIVE, SalesContractStatus.COMPLETED);
    }

    @Transactional
    public void terminate(Long id, String reason) {
        requireStatus(id, SalesContractStatus.ACTIVE);
        contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getStatus, SalesContractStatus.TERMINATED)
                .set(CrmContract::getTerminateReason, reason).eq(CrmContract::getId, id).eq(CrmContract::getStatus, SalesContractStatus.ACTIVE));
    }

    /** 回款计划及各期回款状态：实际回款按计划日期先后分摊 */
    public List<CrmCollectionPlan> plans(CrmContract c) {
        List<CrmCollectionPlan> plans = planMapper.selectList(Wrappers.<CrmCollectionPlan>lambdaQuery()
                .eq(CrmCollectionPlan::getContractId, c.getId()).orderByAsc(CrmCollectionPlan::getPlanDate).orderByAsc(CrmCollectionPlan::getSeq));
        BigDecimal left = c.getReceivedAmount() == null ? BigDecimal.ZERO : c.getReceivedAmount().max(BigDecimal.ZERO);
        LocalDate today = LocalDate.now();
        for (CrmCollectionPlan p : plans) {
            BigDecimal got = left.min(p.getAmount());
            left = left.subtract(got);
            p.setReceivedAmount(got);
            p.setCollectStatus(got.compareTo(p.getAmount()) >= 0 ? CollectStatus.RECEIVED
                    : p.getPlanDate().isBefore(today) ? CollectStatus.OVERDUE : got.signum() > 0 ? CollectStatus.PARTIAL : CollectStatus.PENDING);
            p.setContractNo(c.getContractNo());
            p.setCustomerName(c.getCustomerName());
            p.setOwnerId(c.getOwnerId());
        }
        return plans;
    }

    /** 执行中合同的逾期回款计划（回款提醒） */
    public List<CrmCollectionPlan> overduePlans() {
        List<CrmCollectionPlan> result = new ArrayList<>();
        for (CrmContract c : contractMapper.selectActive()) {
            plans(c).stream().filter(p -> p.getCollectStatus() == CollectStatus.OVERDUE).forEach(result::add);
        }
        return result;
    }

    private void activate(Long id, SalesContractStatus from) {
        if (contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getStatus, SalesContractStatus.ACTIVE)
                .set(CrmContract::getApproveTime, LocalDateTime.now()).eq(CrmContract::getId, id).eq(CrmContract::getStatus, from)) == 0) {
            throw new BizException("crm.error.contractStatusHasChangedRefresh");
        }
    }

    private void fill(CrmContract c, ContractRequest req) {
        customerService.get(req.customerId());
        if (req.endDate() != null && req.startDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BizException("crm.error.endDateCannotEarlierStartDate");
        }
        if (req.opportunityId() != null) {
            CrmOpportunity o = opportunityMapper.selectById(req.opportunityId());
            if (o == null || !o.getCustomerId().equals(req.customerId())) {
                throw new BizException("crm.error.opportunityDoesNotBelongCustomer");
            }
        }
        if (req.quotationId() != null) {
            ScmQuotation q = quotationMapper.selectById(req.quotationId());
            if (q == null || !q.getCustomerId().equals(req.customerId())) {
                throw new BizException("crm.error.quotationDoesNotBelongCustomer");
            }
        }
        if (req.plans() != null) {
            BigDecimal total = req.plans().stream().map(ContractRequest.Plan::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(req.amount()) > 0) {
                throw new BizException("crm.error.collectionPlanTotalExceedsContractAmount", total.toPlainString());
            }
        }
        c.setName(req.name().trim());
        c.setCustomerId(req.customerId());
        c.setOpportunityId(req.opportunityId());
        c.setQuotationId(req.quotationId());
        c.setSignDate(req.signDate());
        c.setStartDate(req.startDate());
        c.setEndDate(req.endDate());
        c.setAmount(req.amount());
        c.setRemark(req.remark());
        if (req.ownerId() == null) {
            LoginUser user = SecurityUtils.getLoginUser();
            c.setOwnerId(user.userId());
            c.setDeptId(user.deptId());
        } else {
            SysUser owner = userMapper.selectById(req.ownerId());
            if (owner == null) {
                throw new BizException("crm.error.ownerDoesNotExist");
            }
            c.setOwnerId(owner.getId());
            c.setDeptId(owner.getDeptId());
        }
    }

    private void savePlans(Long contractId, ContractRequest req) {
        if (req.plans() == null) {
            return;
        }
        int seq = 1;
        for (ContractRequest.Plan p : req.plans()) {
            CrmCollectionPlan plan = new CrmCollectionPlan();
            plan.setContractId(contractId);
            plan.setSeq(seq++);
            plan.setPlanDate(p.planDate());
            plan.setAmount(p.amount());
            plan.setRemark(p.remark());
            planMapper.insert(plan);
        }
    }

    private CrmContract requireStatus(Long id, SalesContractStatus status) {
        getAccessible(id);
        CrmContract c = contractMapper.selectDetail(id);
        if (c.getStatus() != status) {
            throw new BizException("crm.error.contractHasInvalidStatusRefresh", c.getContractNo());
        }
        return c;
    }

    private void transit(Long id, SalesContractStatus from, SalesContractStatus to) {
        if (contractMapper.update(Wrappers.<CrmContract>lambdaUpdate().set(CrmContract::getStatus, to).eq(CrmContract::getId, id)
                .eq(CrmContract::getStatus, from)) == 0) {
            throw new BizException("crm.error.contractStatusHasChangedRefresh");
        }
    }

    private CrmContract getAccessible(Long id) {
        Supplier<CrmContract> loader = () -> contractMapper.selectById(id);
        CrmContract c = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(RULE, loader);
        if (c == null) {
            throw new BizException("crm.error.contractDoesNotExistDoNot");
        }
        return c;
    }
}
