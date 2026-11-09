package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.rule.BizRules;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.OffsetRequests;
import com.lemon.erp.modules.fin.entity.FinBill;
import com.lemon.erp.modules.fin.entity.FinOffset;
import com.lemon.erp.modules.fin.mapper.FinOffsetMapper;
import com.lemon.erp.modules.fin.model.OffsetStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.common.i18n.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 冲账单：登记垫付运费、销售返利、三包赔偿等用来抵扣往来款的项目（不付现金），待复核时可修改、删除。
 * 复核后生成红字应收 / 应付单（来源 OFFSET）并同时生成凭证（应收冲账 借 费用类科目 / 贷 应收账款；应付冲账 借 应付账款 /
 * 贷 收入或冲减成本类科目），在收付款核销时与欠款冲抵；升级前复核、还没有凭证的红字单据仍可在应收应付中手动生成。
 * 取消复核删除红字单据和未审核的凭证，须未核销、凭证未记账。业务参数开启「复核人不能是制单人」时校验。不影响库存
 */
@Service
@RequiredArgsConstructor
public class FinOffsetService {

    /** 冲账单号的编码规则标识 */
    public static final String DOC_NO = "CZ";

    private static final String ITEM_DICT = "fin_offset_item";

    private final FinOffsetMapper offsetMapper;
    private final FinBillService billService;
    private final FinAccountService accountService;
    private final DocNoService docNoService;
    private final DictLookup dictLookup;
    private final BizRules bizRules;

    public PageResult<FinOffset> page(long current, long size, OffsetRequests.Query query) {
        Page<FinOffset> page = Page.of(current, size);
        offsetMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public FinOffset detail(Long id) {
        FinOffset o = offsetMapper.selectDetail(id);
        if (o == null) {
            throw new BizException("fin.error.offsetNotFound");
        }
        return o;
    }

    /** @return 新单据 ID */
    @Transactional
    public Long create(OffsetRequests.Save req) {
        validate(req);
        FinOffset o = new FinOffset();
        o.setOffsetNo(docNoService.next(DOC_NO));
        o.setStatus(OffsetStatus.DRAFT);
        o.setKind(req.kind());
        o.setPartnerId(req.partnerId());
        o.setOffsetDate(req.offsetDate());
        o.setHandlerId(req.handlerId() != null ? req.handlerId() : SecurityUtils.getUserIdOrNull());
        o.setItem(req.item());
        o.setAmount(req.amount());
        o.setBeginDate(req.beginDate());
        o.setEndDate(req.endDate());
        o.setTicketNo(blankToNull(req.ticketNo()));
        o.setAccountId(req.accountId());
        o.setRemark(req.remark());
        offsetMapper.insert(o);
        return o.getId();
    }

    /** 修改（待复核） */
    public void update(Long id, OffsetRequests.Save req) {
        FinOffset old = detail(id);
        validate(req);
        // 经办人不填时与新增一致：保留原经办人，原来也没有时为当前用户
        Long handlerId = req.handlerId() != null ? req.handlerId()
                : old.getHandlerId() != null ? old.getHandlerId() : SecurityUtils.getUserIdOrNull();
        int n = offsetMapper.update(Wrappers.<FinOffset>lambdaUpdate()
                .set(FinOffset::getKind, req.kind())
                .set(FinOffset::getPartnerId, req.partnerId())
                .set(FinOffset::getOffsetDate, req.offsetDate())
                .set(FinOffset::getHandlerId, handlerId)
                .set(FinOffset::getItem, req.item())
                .set(FinOffset::getAmount, req.amount())
                .set(FinOffset::getBeginDate, req.beginDate())
                .set(FinOffset::getEndDate, req.endDate())
                .set(FinOffset::getTicketNo, blankToNull(req.ticketNo()))
                .set(FinOffset::getAccountId, req.accountId())
                .set(FinOffset::getRemark, req.remark())
                .set(FinOffset::getUpdateTime, LocalDateTime.now())
                .eq(FinOffset::getId, id).eq(FinOffset::getStatus, OffsetStatus.DRAFT));
        if (n == 0) {
            throw new BizException("fin.error.offsetOnlyDraftEditable");
        }
    }

    /** 删除（待复核） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            FinOffset o = detail(id);
            if (o.getStatus() != OffsetStatus.DRAFT
                    || offsetMapper.delete(Wrappers.<FinOffset>lambdaQuery().eq(FinOffset::getId, id).eq(FinOffset::getStatus, OffsetStatus.DRAFT)) == 0) {
                throw new BizException("fin.error.offsetOnlyDraftEditable");
            }
        }
    }

    /** 复核：生成红字应收 / 应付单及其凭证，记录复核人 */
    @Transactional
    public void review(Long id) {
        FinOffset o = detail(id);
        if (o.getStatus() != OffsetStatus.DRAFT) {
            throw new BizException("fin.error.offsetAlreadyReviewed", o.getOffsetNo());
        }
        Long userId = SecurityUtils.getUserIdOrNull();
        if (bizRules.bool(FinBizRules.OFFSET_REVIEW_SEPARATE) && userId != null && userId.equals(o.getCreateBy())) {
            throw new BizException("fin.error.offsetReviewerIsCreator", o.getOffsetNo());
        }
        String itemName = dictLookup.options(ITEM_DICT).getOrDefault(o.getItem(), o.getItem());
        String remark = Messages.get(Messages.DEFAULT_LOCALE, "fin.text.offsetBillRemark", o.getOffsetNo(), itemName)
                + (o.getRemark() == null || o.getRemark().isBlank() ? "" : " " + o.getRemark());
        FinBill bill = billService.createOffsetBill(o.getKind(), o.getPartnerId(), o.getId(), o.getOffsetNo(), o.getOffsetDate(),
                o.getAmount(), o.getAccountId(), remark.length() > 255 ? remark.substring(0, 255) : remark);
        StatusTransitions.change(offsetMapper, FinOffset::getId, id, FinOffset::getStatus, OffsetStatus.DRAFT, OffsetStatus.REVIEWED,
                w -> w.set(FinOffset::getBillId, bill.getId()).set(FinOffset::getReviewBy, userId)
                        .set(FinOffset::getReviewTime, LocalDateTime.now()).set(FinOffset::getUpdateTime, LocalDateTime.now()),
                "fin.error.offsetAlreadyReviewed", o.getOffsetNo());
        // 复核即生成凭证（未审核），与红字单据在同一事务
        billService.generateVouchers(List.of(bill.getId()));
    }

    /** 取消复核：删除红字单据和未审核的凭证（已核销、凭证已审核或已记账时不能取消），回到待复核 */
    @Transactional
    public void cancelReview(Long id) {
        FinOffset o = detail(id);
        if (o.getStatus() != OffsetStatus.REVIEWED) {
            throw new BizException("fin.error.offsetNotReviewed", o.getOffsetNo());
        }
        if (o.getBillId() != null) {
            billService.deleteOffsetBill(o.getBillId());
        }
        StatusTransitions.change(offsetMapper, FinOffset::getId, id, FinOffset::getStatus, OffsetStatus.REVIEWED, OffsetStatus.DRAFT,
                w -> w.set(FinOffset::getBillId, null).set(FinOffset::getReviewBy, null).set(FinOffset::getReviewTime, null)
                        .set(FinOffset::getUpdateTime, LocalDateTime.now()),
                "fin.error.offsetNotReviewed", o.getOffsetNo());
    }

    private void validate(OffsetRequests.Save req) {
        // 往来单位必须存在（客户 / 供应商）
        billService.partnerName(req.kind(), req.partnerId());
        dictLookup.check(ITEM_DICT, req.item(), "fin.label.offsetItem");
        if (req.beginDate() != null && req.endDate() != null && req.endDate().isBefore(req.beginDate())) {
            throw new BizException("fin.error.endDateCannotEarlierStartDate");
        }
        if (req.accountId() != null) {
            accountService.requireLeaves(List.of(req.accountId()));
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
