package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.modules.scm.dto.InquiryFinishRequest;
import com.lemon.erp.modules.scm.dto.InquiryQuoteRequest;
import com.lemon.erp.modules.scm.dto.InquiryRequest;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.entity.ScmInquiry;
import com.lemon.erp.modules.scm.entity.ScmInquiryItem;
import com.lemon.erp.modules.scm.entity.ScmInquiryQuote;
import com.lemon.erp.modules.scm.mapper.ScmInquiryItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmInquiryMapper;
import com.lemon.erp.modules.scm.mapper.ScmInquiryQuoteMapper;
import com.lemon.erp.modules.scm.model.InquiryStatus;
import com.lemon.erp.modules.scm.model.OrderDeletedEvent;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.OrderSource;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 询价比价：一张询价单向多个供应商询价，录入报价后逐行比价定标，完成时用中标价更新供货价格、按中标供应商生成采购订单
 */
@Service
@RequiredArgsConstructor
public class ScmInquiryService {

    private static final DataPermissionRule RULE = new DataPermissionRule("scm_inquiry", "dept_id", "create_by");

    private final ScmInquiryMapper inquiryMapper;
    private final ScmInquiryItemMapper itemMapper;
    private final ScmInquiryQuoteMapper quoteMapper;
    private final ScmSupplierService supplierService;
    private final ScmSupplierPriceService supplierPriceService;
    private final ScmOrderService orderService;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;

    @DataPermission(table = "scm_inquiry", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmInquiry> page(long current, long size, String keyword, InquiryStatus status) {
        Page<ScmInquiry> page = Page.of(current, size);
        inquiryMapper.selectPageList(page, keyword, status);
        return PageResult.of(page);
    }

    public ScmInquiry detail(Long id) {
        getAccessible(id);
        ScmInquiry q = inquiryMapper.selectDetail(id);
        q.setItems(inquiryMapper.selectItems(id));
        q.setQuotes(inquiryMapper.selectQuotes(id));
        return q;
    }

    @Transactional
    public Long create(InquiryRequest req) {
        LoginUser user = SecurityUtils.getLoginUser();
        ScmInquiry q = new ScmInquiry();
        q.setInquiryNo(docNoService.next(ScmDocNos.INQUIRY));
        q.setInquiryDate(req.inquiryDate());
        q.setDeadline(req.deadline());
        q.setRemark(req.remark());
        q.setStatus(InquiryStatus.QUOTING);
        q.setDeptId(user.deptId());
        inquiryMapper.insert(q);
        saveItems(q.getId(), req);
        return q.getId();
    }

    /** 已有报价后明细不能再改 */
    @Transactional
    public void update(Long id, InquiryRequest req) {
        getQuoting(id);
        if (quoteMapper.exists(Wrappers.<ScmInquiryQuote>lambdaQuery().eq(ScmInquiryQuote::getInquiryId, id))) {
            throw new BizException("scm.error.suppliersHaveAlreadyQuotedRfqLines");
        }
        inquiryMapper.update(Wrappers.<ScmInquiry>lambdaUpdate().set(ScmInquiry::getInquiryDate, req.inquiryDate())
                .set(ScmInquiry::getDeadline, req.deadline()).set(ScmInquiry::getRemark, req.remark())
                .set(ScmInquiry::getUpdateTime, LocalDateTime.now()).eq(ScmInquiry::getId, id));
        itemMapper.delete(Wrappers.<ScmInquiryItem>lambdaQuery().eq(ScmInquiryItem::getInquiryId, id));
        saveItems(id, req);
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::getQuoting);
        quoteMapper.delete(Wrappers.<ScmInquiryQuote>lambdaQuery().in(ScmInquiryQuote::getInquiryId, ids));
        itemMapper.delete(Wrappers.<ScmInquiryItem>lambdaQuery().in(ScmInquiryItem::getInquiryId, ids));
        inquiryMapper.deleteByIds(ids);
    }

    /** 录入供应商报价：覆盖该供应商之前的报价 */
    @Transactional
    public void saveQuote(Long id, InquiryQuoteRequest req) {
        getQuoting(id);
        supplierService.get(req.supplierId());
        Set<Long> itemIds = itemMapper.selectList(Wrappers.<ScmInquiryItem>lambdaQuery().eq(ScmInquiryItem::getInquiryId, id))
                .stream().map(ScmInquiryItem::getId).collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        for (InquiryQuoteRequest.Line l : req.lines()) {
            if (!itemIds.contains(l.itemId())) {
                throw new BizException("scm.error.quotationLineDoesNotBelongRfq");
            }
            if (!seen.add(l.itemId())) {
                throw new BizException("scm.error.sameLineCannotQuotedMoreOnce");
            }
        }
        List<Long> old = quoteMapper.selectList(Wrappers.<ScmInquiryQuote>lambdaQuery().eq(ScmInquiryQuote::getInquiryId, id)
                .eq(ScmInquiryQuote::getSupplierId, req.supplierId())).stream().map(ScmInquiryQuote::getId).toList();
        if (!old.isEmpty()) {
            // 覆盖报价时清除对旧报价的定标
            itemMapper.update(Wrappers.<ScmInquiryItem>lambdaUpdate().set(ScmInquiryItem::getSelectedQuoteId, null)
                    .eq(ScmInquiryItem::getInquiryId, id).in(ScmInquiryItem::getSelectedQuoteId, old));
            quoteMapper.deleteByIds(old);
        }
        for (InquiryQuoteRequest.Line l : req.lines()) {
            ScmInquiryQuote quote = new ScmInquiryQuote();
            quote.setInquiryId(id);
            quote.setItemId(l.itemId());
            quote.setSupplierId(req.supplierId());
            quote.setPrice(l.price());
            quote.setTaxRate(l.taxRate());
            quote.setLeadDays(l.leadDays());
            quote.setRemark(l.remark());
            quoteMapper.insert(quote);
        }
    }

    @Transactional
    public void deleteQuote(Long id, Long supplierId) {
        saveQuote(id, new InquiryQuoteRequest(supplierId, List.of()));
    }

    /**
     * 定标并完成：中标价可更新到供货价格，按中标供应商合并生成草稿采购订单
     *
     * @return 生成的采购订单 ID
     */
    @Transactional
    public List<Long> finish(Long id, InquiryFinishRequest req) {
        ScmInquiry inquiry = getQuoting(id);
        Map<Long, ScmInquiryItem> items = itemMapper.selectList(Wrappers.<ScmInquiryItem>lambdaQuery().eq(ScmInquiryItem::getInquiryId, id))
                .stream().collect(Collectors.toMap(ScmInquiryItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, ScmInquiryQuote> quotes = quoteMapper.selectList(Wrappers.<ScmInquiryQuote>lambdaQuery().eq(ScmInquiryQuote::getInquiryId, id))
                .stream().collect(Collectors.toMap(ScmInquiryQuote::getId, Function.identity()));
        Map<Long, List<ScmInquiryItem>> bySupplier = new LinkedHashMap<>();
        Map<Long, ScmInquiryQuote> selectedOf = new LinkedHashMap<>();
        Map<Long, Long> selections = req.selections() == null ? Map.of() : req.selections();
        for (Map.Entry<Long, Long> e : selections.entrySet()) {
            ScmInquiryItem item = items.get(e.getKey());
            if (item == null || e.getValue() == null) {
                continue;
            }
            ScmInquiryQuote quote = quotes.get(e.getValue());
            if (quote == null || !quote.getItemId().equals(item.getId())) {
                throw new BizException("scm.error.winningQuotationDoesNotMatchRfq");
            }
            itemMapper.update(Wrappers.<ScmInquiryItem>lambdaUpdate().set(ScmInquiryItem::getSelectedQuoteId, quote.getId())
                    .eq(ScmInquiryItem::getId, item.getId()));
            selectedOf.put(item.getId(), quote);
            bySupplier.computeIfAbsent(quote.getSupplierId(), k -> new ArrayList<>()).add(item);
        }
        if (selectedOf.isEmpty()) {
            throw new BizException("scm.error.selectWinningSupplierLeastOneLine");
        }
        if (req.updatePrice()) {
            selectedOf.forEach((itemId, quote) -> supplierPriceService.applyQuote(quote.getSupplierId(), items.get(itemId).getMaterialId(),
                    quote.getPrice(), quote.getTaxRate(), quote.getLeadDays()));
        }
        List<Long> orderIds = new ArrayList<>();
        if (req.createOrder()) {
            bySupplier.forEach((supplierId, list) -> {
                List<OrderItemRequest> lines = list.stream().map(i -> {
                    ScmInquiryQuote q = selectedOf.get(i.getId());
                    LocalDate delivery = i.getRequiredDate() != null ? i.getRequiredDate()
                            : q.getLeadDays() == null ? null : LocalDate.now().plusDays(q.getLeadDays());
                    return new OrderItemRequest(i.getMaterialId(), i.getQty(), q.getPrice(), q.getTaxRate(), delivery, i.getRemark());
                }).toList();
                Long orderId = orderService.createDraft(OrderKind.PURCHASE, new OrderRequest(supplierId, LocalDate.now(), null,
                        req.warehouseId(), "询价单 " + inquiry.getInquiryNo(), lines), OrderSource.INQUIRY); // i18n-ignore
                itemMapper.update(Wrappers.<ScmInquiryItem>lambdaUpdate().set(ScmInquiryItem::getOrderId, orderId)
                        .in(ScmInquiryItem::getId, list.stream().map(ScmInquiryItem::getId).toList()));
                orderIds.add(orderId);
            });
        }
        transit(id, InquiryStatus.FINISHED);
        return orderIds;
    }

    @Transactional
    public void close(Long id) {
        getQuoting(id);
        transit(id, InquiryStatus.CLOSED);
    }

    /** 生成的草稿采购订单删除后，明细恢复为未下单（询价单保持已完成，可在采购订单中重新下单） */
    @EventListener
    public void onOrderDeleted(OrderDeletedEvent e) {
        if (e.kind() != OrderKind.PURCHASE) {
            return;
        }
        itemMapper.update(Wrappers.<ScmInquiryItem>lambdaUpdate().set(ScmInquiryItem::getOrderId, null).in(ScmInquiryItem::getOrderId, e.orderIds()));
    }

    private void saveItems(Long id, InquiryRequest req) {
        materialService.getEnabled(req.items().stream().map(InquiryRequest.Item::materialId).toList());
        for (InquiryRequest.Item i : req.items()) {
            ScmInquiryItem item = new ScmInquiryItem();
            item.setInquiryId(id);
            item.setMaterialId(i.materialId());
            item.setQty(i.qty());
            item.setRequiredDate(i.requiredDate());
            item.setRemark(i.remark());
            itemMapper.insert(item);
        }
    }

    private void transit(Long id, InquiryStatus to) {
        if (!StatusTransitions.tryChange(inquiryMapper, ScmInquiry::getId, id, ScmInquiry::getStatus, InquiryStatus.QUOTING, to,
                w -> w.set(ScmInquiry::getUpdateTime, LocalDateTime.now()))) {
            throw new BizException("scm.error.rfqStatusHasChangedRefresh");
        }
    }

    private ScmInquiry getQuoting(Long id) {
        ScmInquiry q = getAccessible(id);
        if (q.getStatus() != InquiryStatus.QUOTING) {
            throw new BizException("scm.error.rfqCompletedClosed");
        }
        return q;
    }

    private ScmInquiry getAccessible(Long id) {
        return DataScopes.require(RULE, () -> inquiryMapper.selectById(id), "scm.error.rfqDoesNotExistAccessDenied");
    }
}
