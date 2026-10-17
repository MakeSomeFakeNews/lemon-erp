package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.QuotationRequest;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import com.lemon.erp.modules.scm.entity.ScmQuotation;
import com.lemon.erp.modules.scm.entity.ScmQuotationItem;
import com.lemon.erp.modules.scm.mapper.ScmPriceListMapper;
import com.lemon.erp.modules.scm.mapper.ScmQuotationItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmQuotationMapper;
import com.lemon.erp.modules.scm.model.OrderDeletedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.scm.model.QuotationStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lemon.erp.modules.scm.model.ScmConstants.DEFAULT_TAX_RATE;
import static com.lemon.erp.modules.scm.model.ScmConstants.amount;
import static com.lemon.erp.modules.scm.model.ScmConstants.tax;

/**
 * 销售报价：单价默认取价目表，提交后审批（发布了「销售报价」审批流程时，流程变量含报价金额和最低折扣率），
 * 批准后客户接受即转为销售订单，未成交的标记为失效
 */
@Service
@RequiredArgsConstructor
public class ScmQuotationService {

    public static final String PROCESS_KEY = "scm_quotation";
    private static final ApprovalFlow FLOW = new ApprovalFlow(PROCESS_KEY, "scm_quotation:");
    private static final DataPermissionRule RULE = new DataPermissionRule("scm_quotation", "dept_id", "create_by");

    private final ScmQuotationMapper quotationMapper;
    private final ScmQuotationItemMapper itemMapper;
    private final ScmPriceListMapper priceListMapper;
    private final ScmCustomerService customerService;
    private final ScmOrderService orderService;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final ApprovalSupport approvalSupport;

    @DataPermission(table = "scm_quotation", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmQuotation> page(long current, long size, String keyword, QuotationStatus status, Long customerId, Long opportunityId) {
        Page<ScmQuotation> page = Page.of(current, size);
        quotationMapper.selectPageList(page, keyword, status, customerId, opportunityId);
        return PageResult.of(page);
    }

    public ScmQuotation detail(Long id) {
        getAccessible(id);
        ScmQuotation q = quotationMapper.selectDetail(id);
        q.setItems(quotationMapper.selectItems(id));
        return q;
    }

    /**
     * 按 ID 读取报价单主表，供其他模块（如客户关系的合同）校验报价单归属客户。
     * 不做当前用户的数据权限校验，不含明细
     *
     * @return 报价单，不存在时返回 null
     */
    public ScmQuotation find(Long id) {
        return id == null ? null : quotationMapper.selectById(id);
    }

    @Transactional
    public Long create(QuotationRequest req) {
        LoginUser user = SecurityUtils.getLoginUser();
        ScmQuotation q = new ScmQuotation();
        q.setQuoteNo(docNoService.next(ScmDocNos.QUOTATION));
        q.setStatus(QuotationStatus.DRAFT);
        q.setDeptId(user.deptId());
        fill(q, req);
        quotationMapper.insert(q);
        saveItems(q.getId(), q, req);
        return q.getId();
    }

    @Transactional
    public void update(Long id, QuotationRequest req) {
        getDraft(id);
        ScmQuotation q = new ScmQuotation();
        fill(q, req);
        itemMapper.delete(Wrappers.<ScmQuotationItem>lambdaQuery().eq(ScmQuotationItem::getQuotationId, id));
        saveItems(id, q, req);
        quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate()
                .set(ScmQuotation::getCustomerId, q.getCustomerId())
                .set(ScmQuotation::getOpportunityId, q.getOpportunityId())
                .set(ScmQuotation::getQuoteDate, q.getQuoteDate())
                .set(ScmQuotation::getValidTo, q.getValidTo())
                .set(ScmQuotation::getRemark, q.getRemark())
                .set(ScmQuotation::getTotalAmount, q.getTotalAmount())
                .set(ScmQuotation::getTaxAmount, q.getTaxAmount())
                .set(ScmQuotation::getUpdateTime, LocalDateTime.now())
                .eq(ScmQuotation::getId, id).eq(ScmQuotation::getStatus, QuotationStatus.DRAFT));
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getDraft);
        itemMapper.delete(Wrappers.<ScmQuotationItem>lambdaQuery().in(ScmQuotationItem::getQuotationId, ids));
        quotationMapper.deleteByIds(ids);
    }

    /**
     * 提交：发布了报价审批流程时发起审批，否则直接批准
     *
     * @return true 已发起审批
     */
    @Transactional
    public boolean submit(Long id) {
        ScmQuotation q = getDraft(id);
        Map<String, Object> vars = new HashMap<>();
        vars.put("quoteNo", q.getQuoteNo());
        vars.put("totalAmount", q.getTotalAmount());
        vars.put("discountRate", minDiscountRate(id));
        return approvalSupport.submit(FLOW, id, vars,
                () -> transit(id, QuotationStatus.DRAFT, QuotationStatus.APPROVED, true),
                () -> transit(id, QuotationStatus.DRAFT, QuotationStatus.APPROVING, false),
                instanceId -> quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate().set(ScmQuotation::getFlowInstanceId, instanceId)
                        .eq(ScmQuotation::getId, id)));
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinished(e, (id, approved) -> StatusTransitions.tryChange(quotationMapper, ScmQuotation::getId, id,
                ScmQuotation::getStatus, QuotationStatus.APPROVING, approved ? QuotationStatus.APPROVED : QuotationStatus.DRAFT,
                w -> w.set(approved, ScmQuotation::getApproveBy, SecurityUtils.getUserIdOrNull())
                        .set(approved, ScmQuotation::getApproveTime, LocalDateTime.now())));
    }

    /** 客户接受报价：生成草稿销售订单（来源 QUOTATION） */
    @Transactional
    public Long toOrder(Long id, Long warehouseId, LocalDate deliveryDate) {
        ScmQuotation q = getAccessible(id);
        if (q.getStatus() != QuotationStatus.APPROVED) {
            throw new BizException("scm.error.quotationMustApprovedBeforeConvertingSales");
        }
        if (q.getValidTo() != null && q.getValidTo().isBefore(LocalDate.now())) {
            throw new BizException("scm.error.quotationHasExpiredQuoteAgain");
        }
        List<OrderItemRequest> lines = itemMapper.selectList(Wrappers.<ScmQuotationItem>lambdaQuery()
                        .eq(ScmQuotationItem::getQuotationId, id).orderByAsc(ScmQuotationItem::getId)).stream()
                .map(i -> new OrderItemRequest(i.getMaterialId(), i.getQty(), i.getPrice(), i.getTaxRate(), deliveryDate, i.getRemark()))
                .toList();
        Long orderId = orderService.createDraft(OrderKind.SALES, new OrderRequest(q.getCustomerId(), LocalDate.now(), deliveryDate,
                warehouseId, "报价单 " + q.getQuoteNo(), lines), OrderSource.QUOTATION); // i18n-ignore
        if (!StatusTransitions.tryChange(quotationMapper, ScmQuotation::getId, id, ScmQuotation::getStatus,
                QuotationStatus.APPROVED, QuotationStatus.ORDERED, w -> w.set(ScmQuotation::getOrderId, orderId))) {
            throw new BizException("scm.error.quotationStatusHasChangedRefresh");
        }
        return orderId;
    }

    /** 丢单 / 失效 */
    @Transactional
    public void lose(Long id, String reason) {
        ScmQuotation q = getAccessible(id);
        if (!q.getStatus().isLosable()) {
            throw new BizException("scm.error.onlyDraftApprovedQuotationsMarkedExpired");
        }
        StatusTransitions.tryChange(quotationMapper, ScmQuotation::getId, id, ScmQuotation::getStatus, q.getStatus(), QuotationStatus.LOST,
                w -> w.set(ScmQuotation::getLostReason, reason));
    }

    /** 草稿订单删除后报价恢复为已批准，可以重新转单 */
    @EventListener
    public void onOrderDeleted(OrderDeletedEvent e) {
        if (e.kind() != OrderKind.SALES) {
            return;
        }
        quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate().set(ScmQuotation::getStatus, QuotationStatus.APPROVED)
                .set(ScmQuotation::getOrderId, null).in(ScmQuotation::getOrderId, e.orderIds()));
    }

    private void fill(ScmQuotation q, QuotationRequest req) {
        customerService.get(req.customerId());
        if (req.validTo() != null && req.validTo().isBefore(req.quoteDate())) {
            throw new BizException("scm.error.validUntilDateCannotEarlierQuotation");
        }
        q.setCustomerId(req.customerId());
        q.setOpportunityId(req.opportunityId());
        q.setQuoteDate(req.quoteDate());
        q.setValidTo(req.validTo());
        q.setRemark(req.remark());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (QuotationRequest.Item i : req.items()) {
            BigDecimal a = amount(i.qty(), i.price());
            total = total.add(a);
            taxTotal = taxTotal.add(tax(a, i.taxRate() == null ? DEFAULT_TAX_RATE : i.taxRate()));
        }
        q.setTotalAmount(total);
        q.setTaxAmount(taxTotal);
    }

    private void saveItems(Long id, ScmQuotation q, QuotationRequest req) {
        materialService.getEnabled(req.items().stream().map(QuotationRequest.Item::materialId).toList());
        ScmCustomer customer = customerService.get(q.getCustomerId());
        for (QuotationRequest.Item i : req.items()) {
            ScmPriceList list = priceListMapper.selectQuote(i.materialId(), customer.getId(), customer.getLevel(), i.qty(), q.getQuoteDate());
            ScmQuotationItem item = new ScmQuotationItem();
            item.setQuotationId(id);
            item.setMaterialId(i.materialId());
            item.setQty(i.qty());
            item.setPrice(i.price());
            item.setTaxRate(i.taxRate() == null ? DEFAULT_TAX_RATE : i.taxRate());
            item.setListPrice(list == null ? null : list.getPrice());
            item.setAmount(amount(i.qty(), i.price()));
            item.setRemark(i.remark());
            itemMapper.insert(item);
        }
    }

    /** 最低折扣率（报价 / 价目价 × 100），没有价目价的明细不参与，全部没有时为 100 */
    private BigDecimal minDiscountRate(Long id) {
        return itemMapper.selectList(Wrappers.<ScmQuotationItem>lambdaQuery().eq(ScmQuotationItem::getQuotationId, id)).stream()
                .filter(i -> i.getListPrice() != null && i.getListPrice().signum() > 0)
                .map(i -> Decimals.percent(i.getPrice(), i.getListPrice(), 2))
                .min(BigDecimal::compareTo).orElse(Decimals.HUNDRED);
    }

    private void transit(Long id, QuotationStatus from, QuotationStatus to, boolean approve) {
        boolean changed = StatusTransitions.tryChange(quotationMapper, ScmQuotation::getId, id, ScmQuotation::getStatus, from, to,
                w -> w.set(approve, ScmQuotation::getApproveBy, SecurityUtils.getUserIdOrNull())
                        .set(approve, ScmQuotation::getApproveTime, LocalDateTime.now())
                        .set(ScmQuotation::getUpdateTime, LocalDateTime.now()));
        if (!changed) {
            throw new BizException("scm.error.quotationStatusHasChangedRefresh");
        }
    }

    private ScmQuotation getDraft(Long id) {
        ScmQuotation q = getAccessible(id);
        if (q.getStatus() != QuotationStatus.DRAFT) {
            throw new BizException("scm.error.onlyDraftQuotationsModifiedDeletedSubmitted");
        }
        return q;
    }

    private ScmQuotation getAccessible(Long id) {
        return DataScopes.require(RULE, () -> quotationMapper.selectById(id), "scm.error.quotationDoesNotExistAccessDenied");
    }
}
