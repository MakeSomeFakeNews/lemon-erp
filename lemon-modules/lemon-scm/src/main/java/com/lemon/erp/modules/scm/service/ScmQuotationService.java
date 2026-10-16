package com.lemon.erp.modules.scm.service;

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
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
    private static final String BUSINESS_KEY_PREFIX = "scm_quotation:";
    private static final DataPermissionRule RULE = new DataPermissionRule("scm_quotation", "dept_id", "create_by");
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ScmQuotationMapper quotationMapper;
    private final ScmQuotationItemMapper itemMapper;
    private final ScmPriceListMapper priceListMapper;
    private final ScmCustomerService customerService;
    private final ScmOrderService orderService;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;

    @DataPermission(table = "scm_quotation", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmQuotation> page(long current, long size, String keyword, String status, Long customerId, Long opportunityId) {
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

    @Transactional
    public Long create(QuotationRequest req) {
        LoginUser user = SecurityUtils.getLoginUser();
        ScmQuotation q = new ScmQuotation();
        q.setQuoteNo(docNoService.next("BJ"));
        q.setStatus("DRAFT");
        q.setDeptId(user.deptId());
        q.setCreateBy(user.userId());
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
                .eq(ScmQuotation::getId, id).eq(ScmQuotation::getStatus, "DRAFT"));
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
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            transit(id, "DRAFT", "APPROVED", true);
            return false;
        }
        transit(id, "DRAFT", "APPROVING", false);
        Map<String, Object> vars = new HashMap<>();
        vars.put("quoteNo", q.getQuoteNo());
        vars.put("totalAmount", q.getTotalAmount());
        vars.put("discountRate", minDiscountRate(id));
        String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, vars);
        quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate().set(ScmQuotation::getFlowInstanceId, instanceId)
                .eq(ScmQuotation::getId, id));
        return true;
    }

    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        boolean approved = e.status() == FlowStatus.APPROVED;
        quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate()
                .set(ScmQuotation::getStatus, approved ? "APPROVED" : "DRAFT")
                .set(approved, ScmQuotation::getApproveBy, SecurityUtils.getUserIdOrNull())
                .set(approved, ScmQuotation::getApproveTime, LocalDateTime.now())
                .eq(ScmQuotation::getId, id).eq(ScmQuotation::getStatus, "APPROVING"));
    }

    /** 客户接受报价：生成草稿销售订单（来源 QUOTATION） */
    @Transactional
    public Long toOrder(Long id, Long warehouseId, LocalDate deliveryDate) {
        ScmQuotation q = getAccessible(id);
        if (!"APPROVED".equals(q.getStatus())) {
            throw new BizException("报价单批准后才能转为销售订单");
        }
        if (q.getValidTo() != null && q.getValidTo().isBefore(LocalDate.now())) {
            throw new BizException("报价已过有效期，请重新报价");
        }
        List<OrderItemRequest> lines = itemMapper.selectList(Wrappers.<ScmQuotationItem>lambdaQuery()
                        .eq(ScmQuotationItem::getQuotationId, id).orderByAsc(ScmQuotationItem::getId)).stream()
                .map(i -> new OrderItemRequest(i.getMaterialId(), i.getQty(), i.getPrice(), i.getTaxRate(), deliveryDate, i.getRemark()))
                .toList();
        Long orderId = orderService.createDraft(OrderKind.SALES, new OrderRequest(q.getCustomerId(), LocalDate.now(), deliveryDate,
                warehouseId, "报价单 " + q.getQuoteNo(), lines), "QUOTATION");
        int updated = quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate().set(ScmQuotation::getStatus, "ORDERED")
                .set(ScmQuotation::getOrderId, orderId).eq(ScmQuotation::getId, id).eq(ScmQuotation::getStatus, "APPROVED"));
        if (updated == 0) {
            throw new BizException("报价单状态已变化，请刷新");
        }
        return orderId;
    }

    /** 丢单 / 失效 */
    @Transactional
    public void lose(Long id, String reason) {
        ScmQuotation q = getAccessible(id);
        if (!"APPROVED".equals(q.getStatus()) && !"DRAFT".equals(q.getStatus())) {
            throw new BizException("只能将草稿、已批准的报价单标记为失效");
        }
        quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate().set(ScmQuotation::getStatus, "LOST")
                .set(ScmQuotation::getLostReason, reason).eq(ScmQuotation::getId, id).eq(ScmQuotation::getStatus, q.getStatus()));
    }

    /** 草稿订单删除后报价恢复为已批准，可以重新转单 */
    @EventListener
    public void onOrderDeleted(OrderDeletedEvent e) {
        if (!OrderKind.SALES.name().equals(e.kind())) {
            return;
        }
        quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate().set(ScmQuotation::getStatus, "APPROVED")
                .set(ScmQuotation::getOrderId, null).in(ScmQuotation::getOrderId, e.orderIds()));
    }

    private void fill(ScmQuotation q, QuotationRequest req) {
        customerService.get(req.customerId());
        if (req.validTo() != null && req.validTo().isBefore(req.quoteDate())) {
            throw new BizException("有效期不能早于报价日期");
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
            ScmPriceList list = priceListMapper.selectQuote(i.materialId(), customer.getLevel(), i.qty(), q.getQuoteDate());
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
                .map(i -> i.getPrice().multiply(HUNDRED).divide(i.getListPrice(), 2, RoundingMode.HALF_UP))
                .min(BigDecimal::compareTo).orElse(HUNDRED);
    }

    private void transit(Long id, String from, String to, boolean approve) {
        int updated = quotationMapper.update(Wrappers.<ScmQuotation>lambdaUpdate()
                .set(ScmQuotation::getStatus, to)
                .set(approve, ScmQuotation::getApproveBy, SecurityUtils.getUserIdOrNull())
                .set(approve, ScmQuotation::getApproveTime, LocalDateTime.now())
                .set(ScmQuotation::getUpdateTime, LocalDateTime.now())
                .eq(ScmQuotation::getId, id).eq(ScmQuotation::getStatus, from));
        if (updated == 0) {
            throw new BizException("报价单状态已变化，请刷新");
        }
    }

    private ScmQuotation getDraft(Long id) {
        ScmQuotation q = getAccessible(id);
        if (!"DRAFT".equals(q.getStatus())) {
            throw new BizException("只能修改、删除、提交草稿状态的报价单");
        }
        return q;
    }

    private ScmQuotation getAccessible(Long id) {
        Supplier<ScmQuotation> loader = () -> quotationMapper.selectById(id);
        ScmQuotation q = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(RULE, loader);
        if (q == null) {
            throw new BizException("报价单不存在或没有权限");
        }
        return q;
    }
}
