package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.entity.FinIoDoc;
import com.lemon.erp.modules.fin.entity.FinIoDocItem;
import com.lemon.erp.modules.fin.mapper.FinIoCategoryMapper;
import com.lemon.erp.modules.fin.mapper.FinIoDocItemMapper;
import com.lemon.erp.modules.fin.mapper.FinIoDocMapper;
import com.lemon.erp.modules.fin.model.BillKind;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoPartnerType;
import com.lemon.erp.modules.fin.model.IoStatus;
import com.lemon.erp.modules.fin.model.SettleMethods;
import com.lemon.erp.modules.fin.model.VoucherLine;
import com.lemon.erp.modules.fin.model.VoucherSource;
import com.lemon.erp.modules.fin.model.VoucherStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 其他收支单：其他收入单（IN，单号 SR）、费用支出单（OUT，单号 ZC），明细为收支类别 + 金额。
 * 待确认时可修改、删除；确认时同一事务生成凭证（未审核）：收入 借 结算账户 / 贷 各类别科目，支出 借 各类别科目（带部门）/ 贷 结算账户，
 * 凭证进入出纳日记账和资金日报。取消确认删除未审核的凭证（凭证已审核须先反审核，已记账不能取消）。业务日期所在期间已结账时不能办理
 */
@Service
@RequiredArgsConstructor
public class FinIoService {

    /** 其他收入单、费用支出单的编码规则标识 */
    public static final String INCOME_NO = "SR";
    public static final String EXPENSE_NO = "ZC";

    private static final String SETTLE_METHOD_DICT = "fin_settle_method";
    private static final int MAX_DAYS = 366;

    private final FinIoDocMapper docMapper;
    private final FinIoDocItemMapper itemMapper;
    private final FinIoCategoryMapper categoryMapper;
    private final FinAccountService accountService;
    private final FinBillService billService;
    private final FinVoucherService voucherService;
    private final FinBizPeriodGuard periodGuard;
    private final DocNoService docNoService;
    private final DictLookup dictLookup;
    private final FinFundService fundService;

    public PageResult<FinIoDoc> page(long current, long size, IoRequests.Query query) {
        Page<FinIoDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, query);
        List<Long> ids = page.getRecords().stream().map(FinIoDoc::getId).toList();
        if (!ids.isEmpty()) {
            Map<Long, String> names = docMapper.selectCategoryNames(ids).stream().collect(Collectors.groupingBy(FinIoDocItem::getDocId,
                    LinkedHashMap::new, Collectors.mapping(FinIoDocItem::getCategoryName, Collectors.joining("、"))));
            page.getRecords().forEach(d -> d.setCategoryNames(names.get(d.getId())));
        }
        return PageResult.of(page);
    }

    public FinIoDoc detail(Long id) {
        FinIoDoc d = id == null ? null : docMapper.selectDetail(id);
        if (d == null) {
            throw new BizException("fin.error.ioDocNotFound");
        }
        d.setItems(docMapper.selectItems(id));
        return d;
    }

    /** @return 新单据 ID */
    @Transactional
    public Long create(IoRequests.Save req) {
        List<FinIoDocItem> items = validate(req);
        FinIoDoc d = new FinIoDoc();
        d.setDocNo(docNoService.next(req.direction() == FundDirection.IN ? INCOME_NO : EXPENSE_NO));
        d.setDirection(req.direction());
        d.setStatus(IoStatus.DRAFT);
        fill(d, req, items);
        docMapper.insert(d);
        DetailLines.replace(itemMapper, FinIoDocItem::getDocId, d.getId(), items, FinIoDocItem::setDocId, itemMapper::insertBatch);
        return d.getId();
    }

    /** 修改（待确认）；收入 / 支出方向不能修改 */
    @Transactional
    public void update(Long id, IoRequests.Save req) {
        FinIoDoc old = requireDraft(id);
        if (old.getDirection() != req.direction()) {
            throw new BizException("fin.error.ioDirectionCannotChange");
        }
        periodGuard.checkOpen(old.getDocDate(), "fin.label.incomeExpenseDoc");
        List<FinIoDocItem> items = validate(req);
        FinIoDoc d = new FinIoDoc();
        fill(d, req, items);
        int n = docMapper.update(Wrappers.<FinIoDoc>lambdaUpdate()
                .set(FinIoDoc::getDocDate, d.getDocDate())
                .set(FinIoDoc::getTicketNo, d.getTicketNo())
                .set(FinIoDoc::getPartnerType, d.getPartnerType())
                .set(FinIoDoc::getPartnerId, d.getPartnerId())
                .set(FinIoDoc::getPartnerName, d.getPartnerName())
                .set(FinIoDoc::getSettleMethod, d.getSettleMethod())
                .set(FinIoDoc::getAccountId, d.getAccountId())
                .set(FinIoDoc::getDeptId, d.getDeptId())
                .set(FinIoDoc::getAmount, d.getAmount())
                .set(FinIoDoc::getRemark, d.getRemark())
                .set(FinIoDoc::getUpdateTime, LocalDateTime.now())
                .eq(FinIoDoc::getId, id).eq(FinIoDoc::getStatus, IoStatus.DRAFT));
        if (n == 0) {
            throw new BizException("fin.error.ioDocOnlyDraftEditable", old.getDocNo());
        }
        DetailLines.replace(itemMapper, FinIoDocItem::getDocId, id, items, FinIoDocItem::setDocId, itemMapper::insertBatch);
    }

    /** 删除（待确认） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            FinIoDoc d = requireDraft(id);
            periodGuard.checkOpen(d.getDocDate(), "fin.label.incomeExpenseDoc");
            if (docMapper.delete(Wrappers.<FinIoDoc>lambdaQuery().eq(FinIoDoc::getId, id).eq(FinIoDoc::getStatus, IoStatus.DRAFT)) == 0) {
                throw new BizException("fin.error.ioDocOnlyDraftEditable", d.getDocNo());
            }
            itemMapper.delete(Wrappers.<FinIoDocItem>lambdaQuery().eq(FinIoDocItem::getDocId, id));
        }
    }

    /** 确认：生成凭证（未审核），记录确认人 */
    @Transactional
    public void confirm(Long id) {
        FinIoDoc d = detail(id);
        if (d.getStatus() != IoStatus.DRAFT) {
            throw new BizException("fin.error.ioDocAlreadyConfirmed", d.getDocNo());
        }
        periodGuard.checkOpen(d.getDocDate(), "fin.label.incomeExpenseDoc");
        accountService.requireCashAccount(d.getAccountId());
        Long voucherId = voucherService.createGenerated(d.getDocDate(), VoucherSource.INCOME_EXPENSE, d.getDocNo(), lines(d));
        StatusTransitions.change(docMapper, FinIoDoc::getId, id, FinIoDoc::getStatus, IoStatus.DRAFT, IoStatus.CONFIRMED,
                w -> w.set(FinIoDoc::getVoucherId, voucherId).set(FinIoDoc::getConfirmBy, SecurityUtils.getUserIdOrNull())
                        .set(FinIoDoc::getConfirmTime, LocalDateTime.now()).set(FinIoDoc::getUpdateTime, LocalDateTime.now()),
                "fin.error.ioDocAlreadyConfirmed", d.getDocNo());
    }

    /** 取消确认：删除未审核的凭证，回到待确认（凭证已审核须先反审核，已记账不能取消，可红字冲销凭证） */
    @Transactional
    public void cancelConfirm(Long id) {
        FinIoDoc d = detail(id);
        if (d.getStatus() != IoStatus.CONFIRMED) {
            throw new BizException("fin.error.ioDocNotConfirmed", d.getDocNo());
        }
        periodGuard.checkOpen(d.getDocDate(), "fin.label.incomeExpenseDoc");
        if (d.getVoucherId() != null) {
            if (voucherService.statusOf(d.getVoucherId()) == VoucherStatus.POSTED) {
                throw new BizException("fin.error.ioDocVoucherPosted", d.getDocNo());
            }
            voucherService.deleteOwned(d.getVoucherId());
        }
        StatusTransitions.change(docMapper, FinIoDoc::getId, id, FinIoDoc::getStatus, IoStatus.CONFIRMED, IoStatus.DRAFT,
                w -> w.set(FinIoDoc::getVoucherId, null).set(FinIoDoc::getConfirmBy, null).set(FinIoDoc::getConfirmTime, null)
                        .set(FinIoDoc::getUpdateTime, LocalDateTime.now()),
                "fin.error.ioDocNotConfirmed", d.getDocNo());
    }

    /**
     * 按收支类别汇总（已确认的单据，日期范围不超过一年）；账户调拨的手续费（计入财务费用的实际支出）在支出最后单列一行
     */
    public List<IoRequests.SummaryRow> summary(LocalDate begin, LocalDate end, FundDirection direction) {
        checkRange(begin, end);
        List<IoRequests.SummaryRow> rows = new ArrayList<>(docMapper.selectSummary(begin, end, direction, IoStatus.CONFIRMED));
        IoRequests.SummaryRow fee = direction == FundDirection.IN ? null : fundService.feeSummary(begin, end);
        if (fee != null) {
            rows.add(fee);
        }
        return rows;
    }

    /**
     * 收支明细（已确认的单据的明细行）和账户调拨的手续费
     *
     * @param categoryId  只看该类别
     * @param transferFee 只看账户调拨的手续费
     */
    public PageResult<IoRequests.LineRow> lines(long current, long size, LocalDate begin, LocalDate end, FundDirection direction,
                                                Long categoryId, boolean transferFee) {
        checkRange(begin, end);
        boolean docs = !transferFee;
        boolean fees = categoryId == null && direction != FundDirection.IN;
        Page<IoRequests.LineRow> page = Page.of(current, size);
        if (docs || fees) {
            docMapper.selectLines(page, begin, end, direction, categoryId, IoStatus.CONFIRMED, docs, fees);
        }
        return PageResult.of(page);
    }

    // ---------------------------------------------------------------- 内部

    /** 凭证分录：收入 借 结算账户 / 贷 类别科目；支出 借 类别科目（部门）/ 贷 结算账户。客户 / 供应商记入类别科目分录的辅助核算 */
    private List<VoucherLine> lines(FinIoDoc d) {
        boolean income = d.getDirection() == FundDirection.IN;
        String head = Messages.get(Messages.DEFAULT_LOCALE, income ? "fin.text.ioIncomeSummary" : "fin.text.ioExpenseSummary", d.getDocNo())
                + (d.getPartnerLabel() == null || d.getPartnerLabel().isBlank() ? "" : " " + d.getPartnerLabel());
        List<VoucherLine> lines = new ArrayList<>();
        List<VoucherLine> categoryLines = new ArrayList<>();
        for (FinIoDocItem i : d.getItems()) {
            if (i.getAccountId() == null) {
                throw new BizException("fin.error.ioCategoryNoAccount", i.getCategoryName());
            }
            String summary = cut(head + " " + i.getCategoryName() + (i.getRemark() == null || i.getRemark().isBlank() ? "" : " " + i.getRemark()));
            VoucherLine line = income ? VoucherLine.credit(summary, i.getAccountId(), null, i.getAmount())
                    : VoucherLine.debit(summary, i.getAccountId(), d.getDeptId(), i.getAmount());
            if (d.getPartnerType() == IoPartnerType.CUSTOMER) {
                line = line.withCustomer(d.getPartnerId());
            } else if (d.getPartnerType() == IoPartnerType.SUPPLIER) {
                line = line.withSupplier(d.getPartnerId());
            }
            categoryLines.add(line);
        }
        String summary = cut(head + (d.getRemark() == null || d.getRemark().isBlank() ? "" : " " + d.getRemark()));
        if (income) {
            lines.add(VoucherLine.debit(summary, d.getAccountId(), null, d.getAmount()));
            lines.addAll(categoryLines);
        } else {
            lines.addAll(categoryLines);
            lines.add(VoucherLine.credit(summary, d.getAccountId(), null, d.getAmount()));
        }
        return lines;
    }

    /** 校验并转换明细：类别须启用且与单据方向一致 */
    private List<FinIoDocItem> validate(IoRequests.Save req) {
        accountService.requireCashAccount(req.accountId());
        periodGuard.checkOpen(req.docDate(), "fin.label.incomeExpenseDoc");
        if (SettleMethods.NOTE.equals(req.settleMethod())) {
            throw new BizException("fin.error.ioNoteSettleMethod");
        }
        dictLookup.check(SETTLE_METHOD_DICT, req.settleMethod(), "fin.label.settleMethod");
        if (req.partnerType() == IoPartnerType.CUSTOMER || req.partnerType() == IoPartnerType.SUPPLIER) {
            if (req.partnerId() == null) {
                throw new BizException("fin.error.ioSelectPartner");
            }
            // 客户 / 供应商必须存在
            billService.partnerName(req.partnerType() == IoPartnerType.CUSTOMER ? BillKind.AR : BillKind.AP, req.partnerId());
        }
        Map<Long, FinIoCategory> categories = categoryMapper.selectByIds(req.items().stream().map(IoRequests.Item::categoryId).distinct().toList())
                .stream().collect(Collectors.toMap(FinIoCategory::getId, Function.identity()));
        List<FinIoDocItem> items = new ArrayList<>();
        for (IoRequests.Item i : req.items()) {
            FinIoCategory c = categories.get(i.categoryId());
            if (c == null) {
                throw new BizException("fin.error.ioCategoryNotFound");
            }
            if (c.getDirection() != req.direction()) {
                throw new BizException(req.direction() == FundDirection.IN ? "fin.error.ioCategoryNotIncome" : "fin.error.ioCategoryNotExpense",
                        c.getName());
            }
            if (c.getStatus() != EnableStatus.ENABLED) {
                throw new BizException("fin.error.ioCategoryDisabled", c.getName());
            }
            FinIoDocItem item = new FinIoDocItem();
            item.setCategoryId(i.categoryId());
            item.setAmount(i.amount());
            item.setRemark(blankToNull(i.remark()));
            items.add(item);
        }
        return items;
    }

    private static void fill(FinIoDoc d, IoRequests.Save req, List<FinIoDocItem> items) {
        boolean partner = req.partnerType() == IoPartnerType.CUSTOMER || req.partnerType() == IoPartnerType.SUPPLIER;
        d.setDocDate(req.docDate());
        d.setTicketNo(blankToNull(req.ticketNo()));
        d.setPartnerType(req.partnerType() == IoPartnerType.OTHER && blankToNull(req.partnerName()) == null ? null : req.partnerType());
        d.setPartnerId(partner ? req.partnerId() : null);
        d.setPartnerName(req.partnerType() == IoPartnerType.OTHER ? blankToNull(req.partnerName()) : null);
        d.setSettleMethod(blankToNull(req.settleMethod()));
        d.setAccountId(req.accountId());
        d.setDeptId(req.deptId());
        d.setAmount(Decimals.sum(items, FinIoDocItem::getAmount));
        d.setRemark(blankToNull(req.remark()));
    }

    private FinIoDoc requireDraft(Long id) {
        FinIoDoc d = detail(id);
        if (d.getStatus() != IoStatus.DRAFT) {
            throw new BizException("fin.error.ioDocOnlyDraftEditable", d.getDocNo());
        }
        return d;
    }

    private static void checkRange(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new BizException("fin.error.selectDateRange");
        }
        if (end.isBefore(begin)) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("fin.error.dateRangeCannotExceedOneYear");
        }
    }

    private static String cut(String s) {
        return s.length() > 255 ? s.substring(0, 255) : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

}
