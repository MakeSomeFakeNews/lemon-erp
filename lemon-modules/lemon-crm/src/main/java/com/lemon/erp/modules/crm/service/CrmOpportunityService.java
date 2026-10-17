package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.modules.crm.dto.OppOrderRequest;
import com.lemon.erp.modules.crm.dto.OpportunityRequest;
import com.lemon.erp.modules.crm.dto.StageRequest;
import com.lemon.erp.modules.crm.entity.CrmContact;
import com.lemon.erp.modules.crm.entity.CrmOpportunity;
import com.lemon.erp.modules.crm.entity.CrmOpportunityItem;
import com.lemon.erp.modules.crm.entity.CrmStageLog;
import com.lemon.erp.modules.crm.mapper.CrmContactMapper;
import com.lemon.erp.modules.crm.mapper.CrmOpportunityItemMapper;
import com.lemon.erp.modules.crm.mapper.CrmOpportunityMapper;
import com.lemon.erp.modules.crm.mapper.CrmStageLogMapper;
import com.lemon.erp.modules.crm.model.CrmConstants;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import com.lemon.erp.modules.scm.model.OrderDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 销售机会（商机）：初步接洽 → 需求确认 → 方案报价 → 商务谈判 → 赢单 / 输单，每次阶段变化记录日志用于转化率分析。
 * 赢单后客户阶段变为成交，可以按产品明细生成销售订单（草稿，来源「商机」）。数据权限按负责人部门 + 负责人过滤
 */
@Service
@RequiredArgsConstructor
public class CrmOpportunityService {

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    private final CrmOpportunityMapper opportunityMapper;
    private final CrmOpportunityItemMapper itemMapper;
    private final CrmStageLogMapper stageLogMapper;
    private final CrmContactMapper contactMapper;
    private final CrmCustomerService customerService;
    private final ScmOrderService orderService;
    private final ScmOrderMapper orderMapper;
    private final WmsMaterialService materialService;
    private final SysUserMapper userMapper;
    private final DocNoService docNoService;

    @DataPermission(table = "crm_opportunity", deptColumn = "dept_id", userColumn = "owner_id")
    public PageResult<CrmOpportunity> page(long current, long size, String keyword, String stage, String status,
                                           Long customerId, Long ownerId, Long campaignId) {
        Page<CrmOpportunity> page = Page.of(current, size);
        opportunityMapper.selectPageList(page, keyword, stage, status, customerId, ownerId, campaignId);
        return PageResult.of(page);
    }

    public CrmOpportunity get(Long id) {
        CrmOpportunity o = id == null ? null : opportunityMapper.selectById(id);
        if (o == null) {
            throw new BizException("crm.error.opportunityDoesNotExist");
        }
        return o;
    }

    public CrmOpportunity detail(Long id) {
        CrmOpportunity o = opportunityMapper.selectDetail(id);
        if (o == null) {
            throw new BizException("crm.error.opportunityDoesNotExist");
        }
        o.setItems(itemMapper.selectByOpportunity(id));
        return o;
    }

    public List<CrmStageLog> stageLogs(Long id) {
        return stageLogMapper.selectByOpportunity(id);
    }

    @Transactional
    public Long create(OpportunityRequest req) {
        return create(req, null);
    }

    /** @param leadId 来源线索（线索转化时） */
    @Transactional
    public Long create(OpportunityRequest req, Long leadId) {
        customerService.detail(req.customerId());
        CrmOpportunity o = new CrmOpportunity();
        List<CrmOpportunityItem> items = fill(o, req);
        String stage = req.stage() != null ? req.stage() : CrmConstants.OPEN_STAGES.get(0);
        // 新建只能是进行中的阶段，赢单 / 输单在商机详情中推进阶段时处理
        if (!CrmConstants.OPEN_STAGES.contains(stage)) {
            throw new BizException("crm.error.createStageMustBeOpen");
        }
        o.setOppNo(docNoService.next("SJ"));
        o.setStage(stage);
        if (o.getProbability() == null) o.setProbability(CrmConstants.STAGE_PROBABILITY.get(stage));
        o.setLeadId(leadId);
        o.setCreateBy(SecurityUtils.getUserId());
        opportunityMapper.insert(o);
        saveItems(o.getId(), items);
        log(o.getId(), null, stage);
        return o.getId();
    }

    @Transactional
    public void update(Long id, OpportunityRequest req) {
        CrmOpportunity old = getOpen(id);
        CrmOpportunity o = new CrmOpportunity();
        List<CrmOpportunityItem> items = fill(o, req);
        opportunityMapper.update(Wrappers.<CrmOpportunity>lambdaUpdate()
                .set(CrmOpportunity::getName, o.getName())
                .set(CrmOpportunity::getCustomerId, o.getCustomerId())
                .set(CrmOpportunity::getContactId, o.getContactId())
                .set(CrmOpportunity::getAmount, o.getAmount())
                .set(CrmOpportunity::getProbability, o.getProbability() != null ? o.getProbability() : old.getProbability())
                .set(CrmOpportunity::getExpectedDate, o.getExpectedDate())
                .set(CrmOpportunity::getSource, o.getSource())
                .set(CrmOpportunity::getCampaignId, o.getCampaignId())
                .set(CrmOpportunity::getOwnerId, o.getOwnerId())
                .set(CrmOpportunity::getDeptId, o.getDeptId())
                .set(CrmOpportunity::getRemark, o.getRemark())
                .set(CrmOpportunity::getUpdateTime, LocalDateTime.now())
                .eq(CrmOpportunity::getId, id));
        itemMapper.delete(Wrappers.<CrmOpportunityItem>lambdaQuery().eq(CrmOpportunityItem::getOpportunityId, id));
        saveItems(id, items);
    }

    /**
     * 推进销售阶段：赢单 / 输单为最终阶段，赢单后客户变为成交客户；输单可以重新打开（回到商务谈判）
     */
    @Transactional
    public void changeStage(Long id, StageRequest req) {
        CrmOpportunity o = get(id);
        String to = req.stage();
        if (!CrmConstants.STAGES.contains(to)) {
            throw new BizException("crm.error.invalidSalesStage");
        }
        if (to.equals(o.getStage())) {
            return;
        }
        if ("WON".equals(o.getStage())) {
            throw new BizException("crm.error.stageWonOpportunityCannotChanged");
        }
        if ("LOST".equals(to) && !StringUtils.hasText(req.lostReason())) {
            throw new BizException("crm.error.enterLostReason");
        }
        boolean closed = "WON".equals(to) || "LOST".equals(to);
        opportunityMapper.update(Wrappers.<CrmOpportunity>lambdaUpdate()
                .set(CrmOpportunity::getStage, to)
                .set(CrmOpportunity::getProbability, CrmConstants.STAGE_PROBABILITY.get(to))
                .set(CrmOpportunity::getCloseDate, closed ? LocalDate.now() : null)
                .set(CrmOpportunity::getLostReason, "LOST".equals(to) ? req.lostReason() : null)
                .set(CrmOpportunity::getUpdateTime, LocalDateTime.now())
                .eq(CrmOpportunity::getId, id)
                .eq(CrmOpportunity::getStage, o.getStage()));
        log(id, o.getStage(), to);
        if ("WON".equals(to)) {
            customerService.markDeal(o.getCustomerId());
        }
    }

    /** 生成的销售订单（草稿）被删除后，商机解除关联，可以重新生成订单或删除 */
    @EventListener(condition = "#e.kind == 'SALES'")
    public void onOrderDeleted(OrderDeletedEvent e) {
        opportunityMapper.update(Wrappers.<CrmOpportunity>lambdaUpdate()
                .set(CrmOpportunity::getSalesOrderId, null).set(CrmOpportunity::getSalesOrderNo, null)
                .in(CrmOpportunity::getSalesOrderId, e.orderIds()));
    }

    /** 生成过销售订单的商机不能删除 */
    @Transactional
    public void delete(List<Long> ids) {
        for (CrmOpportunity o : opportunityMapper.selectByIds(ids)) {
            if (o.getSalesOrderId() != null) {
                throw new BizException("crm.error.opportunityAlreadyHasSalesOrderCannot", o.getName());
            }
        }
        itemMapper.delete(Wrappers.<CrmOpportunityItem>lambdaQuery().in(CrmOpportunityItem::getOpportunityId, ids));
        stageLogMapper.delete(Wrappers.<CrmStageLog>lambdaQuery().in(CrmStageLog::getOpportunityId, ids));
        opportunityMapper.deleteByIds(ids);
    }

    /**
     * 赢单商机按产品明细生成销售订单（草稿，来源「商机」），订单审核后即可出库
     *
     * @return 销售订单 ID
     */
    @Transactional
    public Long createOrder(Long id, OppOrderRequest req) {
        CrmOpportunity o = detail(id);
        if (!"WON".equals(o.getStage())) {
            throw new BizException("crm.error.salesOrderOnlyCreatedAfterOpportunity");
        }
        if (o.getSalesOrderId() != null && orderMapper.selectById(o.getSalesOrderId()) != null) {
            throw new BizException("crm.error.salesOrderHasAlreadyCreated", o.getSalesOrderNo());
        }
        if (o.getItems().isEmpty()) {
            throw new BizException("crm.error.opportunityHasNoProductLinesAdd");
        }
        List<OrderItemRequest> items = o.getItems().stream()
                .map(i -> new OrderItemRequest(i.getMaterialId(), i.getQty(), i.getPrice(), i.getTaxRate(), null, null))
                .toList();
        Long orderId = orderService.createDraft(OrderKind.SALES, new OrderRequest(o.getCustomerId(), LocalDate.now(),
                req.deliveryDate() != null ? req.deliveryDate() : o.getExpectedDate(), req.warehouseId(),
                "商机 " + o.getOppNo() + " " + o.getName(), items), "CRM"); // i18n-ignore 存库的订单备注
        ScmOrder order = orderMapper.selectById(orderId);
        opportunityMapper.update(Wrappers.<CrmOpportunity>lambdaUpdate()
                .set(CrmOpportunity::getSalesOrderId, orderId)
                .set(CrmOpportunity::getSalesOrderNo, order.getOrderNo())
                .eq(CrmOpportunity::getId, id));
        return orderId;
    }

    // ---------------------------------------------------------------- 内部

    private CrmOpportunity getOpen(Long id) {
        CrmOpportunity o = get(id);
        if ("WON".equals(o.getStage()) || "LOST".equals(o.getStage())) {
            throw new BizException("crm.error.wonLostOpportunitiesCannotModified");
        }
        return o;
    }

    private List<CrmOpportunityItem> fill(CrmOpportunity o, OpportunityRequest req) {
        if (req.contactId() != null) {
            CrmContact contact = contactMapper.selectById(req.contactId());
            if (contact == null || !Objects.equals(contact.getCustomerId(), req.customerId())) {
                throw new BizException("crm.error.contactDoesNotBelongCustomer");
            }
        }
        if (req.items() != null && !req.items().isEmpty()) {
            materialService.getEnabled(req.items().stream().map(OpportunityRequest.Item::materialId).toList());
        }
        List<CrmOpportunityItem> items = req.items() == null ? List.of() : req.items().stream().map(i -> {
            CrmOpportunityItem item = new CrmOpportunityItem();
            item.setMaterialId(i.materialId());
            item.setQty(i.qty());
            item.setPrice(i.price());
            item.setTaxRate(i.taxRate() == null ? DEFAULT_TAX_RATE : i.taxRate());
            item.setAmount(i.qty().multiply(i.price()).setScale(2, RoundingMode.HALF_UP));
            return item;
        }).toList();
        o.setName(req.name().trim());
        o.setCustomerId(req.customerId());
        o.setContactId(req.contactId());
        o.setAmount(items.isEmpty() ? (req.amount() == null ? BigDecimal.ZERO : req.amount())
                : items.stream().map(CrmOpportunityItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        o.setProbability(req.probability());
        o.setExpectedDate(req.expectedDate());
        o.setSource(req.source());
        o.setCampaignId(req.campaignId());
        o.setRemark(req.remark());
        if (req.ownerId() == null) {
            LoginUser user = SecurityUtils.getLoginUser();
            o.setOwnerId(user.userId());
            o.setDeptId(user.deptId());
        } else {
            SysUser owner = userMapper.selectById(req.ownerId());
            if (owner == null) {
                throw new BizException("crm.error.ownerDoesNotExist");
            }
            o.setOwnerId(owner.getId());
            o.setDeptId(owner.getDeptId());
        }
        return items;
    }

    private void saveItems(Long id, List<CrmOpportunityItem> items) {
        if (items.isEmpty()) {
            return;
        }
        items.forEach(i -> i.setOpportunityId(id));
        itemMapper.insertBatch(items);
    }

    private void log(Long id, String from, String to) {
        CrmStageLog log = new CrmStageLog();
        log.setOpportunityId(id);
        log.setFromStage(from);
        log.setToStage(to);
        log.setUserId(SecurityUtils.getUserIdOrNull());
        stageLogMapper.insert(log);
    }
}
