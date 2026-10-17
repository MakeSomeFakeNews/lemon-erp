package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.crm.dto.TicketActionRequest;
import com.lemon.erp.modules.crm.dto.TicketChargeRequest;
import com.lemon.erp.modules.crm.dto.TicketPartsRequest;
import com.lemon.erp.modules.crm.dto.TicketRequest;
import com.lemon.erp.modules.crm.dto.WarrantyInfo;
import com.lemon.erp.modules.crm.entity.CrmContact;
import com.lemon.erp.modules.crm.entity.CrmTicket;
import com.lemon.erp.modules.crm.entity.CrmTicketLog;
import com.lemon.erp.modules.crm.entity.CrmTicketPart;
import com.lemon.erp.modules.crm.mapper.CrmContactMapper;
import com.lemon.erp.modules.crm.mapper.CrmTicketLogMapper;
import com.lemon.erp.modules.crm.mapper.CrmTicketMapper;
import com.lemon.erp.modules.crm.mapper.CrmTicketPartMapper;
import com.lemon.erp.modules.crm.model.CrmConstants;
import com.lemon.erp.modules.crm.model.TicketAction;
import com.lemon.erp.modules.crm.model.TicketStatus;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.BillSource;
import com.lemon.erp.modules.fin.service.FinBillService;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 服务工单：受理（按优先级计算处理时限）→ 指派 → 处理中 → 已解决 → 关闭（客户满意度），已解决、已关闭的可以重新打开。
 * 报修按产品序列号追溯销售出库日期，判断是否在保修期内
 */
@Service
@RequiredArgsConstructor
public class CrmTicketService {

    private final CrmTicketMapper ticketMapper;
    private final CrmTicketLogMapper logMapper;
    private final CrmContactMapper contactMapper;
    private final CrmCustomerService customerService;
    private final DocNoService docNoService;
    private final CrmTicketPartMapper partMapper;
    private final WmsStockDocService stockDocService;
    private final FinBillService billService;
    private final DictLookup dictLookup;

    public PageResult<CrmTicket> page(long current, long size, String keyword, String type, String priority, String status,
                                      Long customerId, Long assigneeId, Boolean overdue) {
        Page<CrmTicket> page = Page.of(current, size);
        ticketMapper.selectPageList(page, keyword, type, priority, status, customerId, assigneeId, overdue);
        return PageResult.of(page);
    }

    public CrmTicket detail(Long id) {
        CrmTicket t = ticketMapper.selectDetail(id);
        if (t == null) {
            throw new BizException("crm.error.ticketDoesNotExist");
        }
        return t;
    }

    public List<CrmTicketLog> logs(Long id) {
        return logMapper.selectByTicket(id);
    }

    @Transactional
    public Long create(TicketRequest req) {
        check(req);
        LoginUser user = SecurityUtils.getLoginUser();
        CrmTicket t = new CrmTicket();
        fill(t, req);
        t.setTicketNo(docNoService.next(CrmDocNos.TICKET));
        t.setStatus(TicketStatus.OPEN);
        t.setDueTime(LocalDateTime.now().withNano(0).plusHours(req.priority().getSlaHours()));
        t.setDeptId(user.deptId());
        ticketMapper.insert(t);
        log(t.getId(), TicketAction.CREATE, req.title());
        if (req.assigneeId() != null) {
            log(t.getId(), TicketAction.ASSIGN, null);
        }
        return t.getId();
    }

    /** 修改未解决的工单；修改优先级时按受理时间重新计算处理时限 */
    @Transactional
    public void update(Long id, TicketRequest req) {
        CrmTicket old = getActive(id);
        check(req);
        CrmTicket t = new CrmTicket();
        fill(t, req);
        ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate()
                .set(CrmTicket::getCustomerId, t.getCustomerId())
                .set(CrmTicket::getContactId, t.getContactId())
                .set(CrmTicket::getType, t.getType())
                .set(CrmTicket::getPriority, t.getPriority())
                .set(CrmTicket::getChannel, t.getChannel())
                .set(CrmTicket::getTitle, t.getTitle())
                .set(CrmTicket::getDescription, t.getDescription())
                .set(CrmTicket::getMaterialId, t.getMaterialId())
                .set(CrmTicket::getSerialNo, t.getSerialNo())
                .set(CrmTicket::getAssigneeId, t.getAssigneeId())
                .set(CrmTicket::getDueTime, old.getCreateTime().plusHours(req.priority().getSlaHours()))
                .set(CrmTicket::getUpdateTime, LocalDateTime.now())
                .eq(CrmTicket::getId, id));
    }

    @Transactional
    public void assign(Long id, TicketActionRequest req) {
        getActive(id);
        if (req.assigneeId() == null) {
            throw new BizException("crm.error.selectAssignee");
        }
        ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate().set(CrmTicket::getAssigneeId, req.assigneeId()).eq(CrmTicket::getId, id));
        log(id, TicketAction.ASSIGN, req.content());
    }

    /** 添加处理记录，待处理变为处理中 */
    @Transactional
    public void process(Long id, TicketActionRequest req) {
        CrmTicket t = getActive(id);
        requireContent(req, "crm.error.enterProcessingNote");
        if (t.getStatus() == TicketStatus.OPEN) {
            ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate()
                    .set(CrmTicket::getStatus, TicketStatus.PROCESSING)
                    .set(t.getAssigneeId() == null, CrmTicket::getAssigneeId, SecurityUtils.getUserId())
                    .eq(CrmTicket::getId, id).eq(CrmTicket::getStatus, TicketStatus.OPEN));
        }
        log(id, TicketAction.PROCESS, req.content());
    }

    @Transactional
    public void resolve(Long id, TicketActionRequest req) {
        getActive(id);
        requireContent(req, "crm.error.enterSolution");
        ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate()
                .set(CrmTicket::getStatus, TicketStatus.RESOLVED)
                .set(CrmTicket::getSolution, req.content())
                .set(CrmTicket::getResolvedTime, LocalDateTime.now())
                .eq(CrmTicket::getId, id).in(CrmTicket::getStatus, TicketStatus.ACTIVE));
        log(id, TicketAction.RESOLVE, req.content());
    }

    /** 关闭：记录客户满意度 */
    @Transactional
    public void close(Long id, TicketActionRequest req) {
        CrmTicket t = detail(id);
        if (t.getStatus() != TicketStatus.RESOLVED) {
            throw new BizException("crm.error.ticketOnlyClosedAfterResolved");
        }
        ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate()
                .set(CrmTicket::getStatus, TicketStatus.CLOSED)
                .set(CrmTicket::getSatisfaction, req.satisfaction())
                .set(CrmTicket::getClosedTime, LocalDateTime.now())
                .eq(CrmTicket::getId, id).eq(CrmTicket::getStatus, TicketStatus.RESOLVED));
        log(id, TicketAction.CLOSE, req.satisfaction() == null ? req.content() : "满意度 " + req.satisfaction() + (req.content() == null ? "" : "：" + req.content())); // i18n-ignore 存库的处理记录
    }

    /** 重新打开：客户反馈问题未解决 */
    @Transactional
    public void reopen(Long id, TicketActionRequest req) {
        CrmTicket t = detail(id);
        if (t.getStatus().isActive()) {
            throw new BizException("crm.error.ticketNotResolvedDoesNotNeed");
        }
        requireContent(req, "crm.error.enterReasonReopening");
        ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate()
                .set(CrmTicket::getStatus, TicketStatus.PROCESSING)
                .set(CrmTicket::getResolvedTime, null)
                .set(CrmTicket::getClosedTime, null)
                .set(CrmTicket::getSatisfaction, null)
                .eq(CrmTicket::getId, id));
        log(id, TicketAction.REOPEN, req.content());
    }

    /** 只能删除待处理的工单（误录） */
    @Transactional
    public void delete(List<Long> ids) {
        for (CrmTicket t : ticketMapper.selectByIds(ids)) {
            if (t.getStatus() != TicketStatus.OPEN) {
                throw new BizException("crm.error.ticketAlreadyProcessedCannotDeleted", t.getTicketNo());
            }
        }
        logMapper.delete(Wrappers.<CrmTicketLog>lambdaQuery().in(CrmTicketLog::getTicketId, ids));
        ticketMapper.deleteByIds(ids);
    }

    /** 序列号的销售与保修信息，没有销售出库记录时返回 null */
    public WarrantyInfo warranty(String serialNo) {
        if (!StringUtils.hasText(serialNo)) {
            return null;
        }
        List<Map<String, Object>> rows = ticketMapper.selectSerialSales(serialNo.trim());
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.get(0);
        LocalDate salesDate = LocalDate.parse(r.get("bizDate").toString().substring(0, 10));
        LocalDate end = salesDate.plusMonths(CrmConstants.WARRANTY_MONTHS);
        return new WarrantyInfo(serialNo.trim(), ((Number) r.get("materialId")).longValue(), (String) r.get("materialCode"),
                (String) r.get("materialName"), (String) r.get("docNo"), salesDate, ((Number) r.get("customerId")).longValue(),
                (String) r.get("customerName"), end, !LocalDate.now().isAfter(end));
    }

    // ---------------------------------------------------------------- 内部

    private CrmTicket getActive(Long id) {
        CrmTicket t = detail(id);
        if (!t.getStatus().isActive()) {
            throw new BizException("crm.error.ticketHasAlreadyResolvedClosed");
        }
        return t;
    }

    private void check(TicketRequest req) {
        customerService.detail(req.customerId());
        // 工单类型、受理渠道是业务字典（租户可新增），优先级是系统字典（决定处理时限）
        dictLookup.check("crm_ticket_type", req.type(), "crm.label.ticketType");
        dictLookup.check("crm_ticket_channel", req.channel(), "crm.label.ticketChannel");
        if (req.contactId() != null) {
            CrmContact contact = contactMapper.selectById(req.contactId());
            if (contact == null || !Objects.equals(contact.getCustomerId(), req.customerId())) {
                throw new BizException("crm.error.contactDoesNotBelongCustomer");
            }
        }
    }

    private static void requireContent(TicketActionRequest req, String messageKey) {
        if (req == null || !StringUtils.hasText(req.content())) {
            throw new BizException(messageKey);
        }
    }

    private static void fill(CrmTicket t, TicketRequest req) {
        t.setCustomerId(req.customerId());
        t.setContactId(req.contactId());
        t.setType(req.type());
        t.setPriority(req.priority());
        t.setChannel(req.channel());
        t.setTitle(req.title().trim());
        t.setDescription(req.description());
        t.setMaterialId(req.materialId());
        t.setSerialNo(StringUtils.hasText(req.serialNo()) ? req.serialNo().trim() : null);
        t.setAssigneeId(req.assigneeId());
    }

    public List<CrmTicketPart> parts(Long id) {
        detail(id);
        return partMapper.selectByTicket(id);
    }

    /** 领用备件：生成服务领用出库单（直接确认），记录到工单 */
    @Transactional
    public Long issueParts(Long id, TicketPartsRequest req) {
        CrmTicket t = detail(id);
        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new BizException("crm.error.ticketClosedSparePartsCannotIssued");
        }
        WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.SERVICE.name(), req.warehouseId(), null,
                req.bizDate(), req.remark() == null ? "服务工单 " + t.getTicketNo() + " 领用备件" : req.remark(), req.items()), t.getTicketNo()); // i18n-ignore 存库的单据备注
        CrmTicketPart p = new CrmTicketPart();
        p.setTicketId(id);
        p.setWmsDocId(doc.getId());
        p.setDocNo(doc.getDocNo());
        p.setWarehouseId(req.warehouseId());
        p.setBizDate(req.bizDate());
        p.setTotalQty(Decimals.sum(req.items(), StockDocItemRequest::qty));
        p.setRemark(req.remark());
        partMapper.insert(p);
        log(id, TicketAction.PROCESS, "领用备件，出库单 " + doc.getDocNo()); // i18n-ignore 存库的处理记录
        return p.getId();
    }

    /** 服务收费：为客户生成应收单（借 应收账款 / 贷 其他业务收入、销项税额），每张工单收一次 */
    @Transactional
    public void charge(Long id, TicketChargeRequest req) {
        CrmTicket t = detail(id);
        if (t.getFeeBillId() != null) {
            throw new BizException("crm.error.ticketHasAlreadyCharged");
        }
        BigDecimal rate = req.taxRate() == null ? CrmConstants.SERVICE_TAX_RATE : req.taxRate();
        BigDecimal tax = req.amount().multiply(rate).divide(Decimals.HUNDRED.add(rate), 2, RoundingMode.HALF_UP);
        FinBill bill = billService.createFromSource(BillKind.AR, t.getCustomerId(), BillSource.TICKET, id, t.getTicketNo(), req.billDate(),
                req.amount(), tax, req.remark() == null ? "服务费 " + t.getTitle() : req.remark(), null); // i18n-ignore 存库的单据摘要
        ticketMapper.update(Wrappers.<CrmTicket>lambdaUpdate().set(CrmTicket::getFeeAmount, req.amount())
                .set(CrmTicket::getFeeBillId, bill.getId()).eq(CrmTicket::getId, id));
        log(id, TicketAction.PROCESS, "服务收费 ¥" + req.amount().stripTrailingZeros().toPlainString() + "，应收单 " + bill.getBillNo()); // i18n-ignore 存库的处理记录
    }

    private void log(Long ticketId, TicketAction action, String content) {
        CrmTicketLog log = new CrmTicketLog();
        log.setTicketId(ticketId);
        log.setAction(action);
        log.setContent(content);
        log.setUserId(SecurityUtils.getUserIdOrNull());
        logMapper.insert(log);
    }
}
