package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.modules.fin.entity.FinStatementBase;
import com.lemon.erp.modules.fin.model.StatementStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 对账单（供应商对账单、客户对账单）共用的状态流转：生成为草稿 → 发送（草稿或有异议的）→ 对方在门户确认或提出异议（须说明理由）；
 * 草稿和有异议的可以删除后重新生成，有异议的也可以核对后再次发送。门户账号只能看到本单位已发送的对账单。
 * 每种对账单声明一个常量，给出实体的主键、状态、对方意见、确认时间字段和往来单位
 *
 * @param partnerRemarkColumn 对方确认或提出异议时填写的意见
 * @param partnerOf           对账单的往来单位（供应商 / 客户）
 */
public record StatementFlow<T extends FinStatementBase>(SFunction<T, ?> idColumn, SFunction<T, StatementStatus> statusColumn,
                                                         SFunction<T, String> partnerRemarkColumn,
                                                         SFunction<T, LocalDateTime> confirmTimeColumn, Function<T, Long> partnerOf) {

    private static final String CHANGED = "fin.error.statementStatusHasChangedRefreshTry";

    /**
     * 不存在、或门户账号访问其他单位 / 草稿时提示不存在
     *
     * @param portalPartnerId 门户账号关联的往来单位，内部用户为 null
     */
    public T check(T st, Long portalPartnerId) {
        if (st == null || portalPartnerId != null
                && (!portalPartnerId.equals(partnerOf.apply(st)) || st.getStatus() == StatementStatus.DRAFT)) {
            throw new BizException("fin.error.statementNotFound");
        }
        return st;
    }

    /** 门户账号（附件等）能否访问：本单位已发送的；内部用户（portalPartnerId 为 null）都能访问 */
    public boolean visible(T st, Long portalPartnerId) {
        return st != null && (portalPartnerId == null
                || portalPartnerId.equals(partnerOf.apply(st)) && st.getStatus() != StatementStatus.DRAFT);
    }

    /** 草稿和有异议的可以发送、删除 */
    public static boolean editable(StatementStatus status) {
        return status == StatementStatus.DRAFT || status == StatementStatus.DISPUTED;
    }

    /** 发送给对方确认（草稿或有异议的） */
    public void send(BaseMapper<T> mapper, T st) {
        if (!editable(st.getStatus())) {
            throw new BizException("fin.error.statementHasAlreadySentConfirmed");
        }
        StatusTransitions.change(mapper, idColumn, st.getId(), statusColumn, st.getStatus(), StatementStatus.SENT, CHANGED);
    }

    /** 删除前检查：只能删除草稿或有异议的 */
    public void checkDeletable(T st) {
        if (!editable(st.getStatus())) {
            throw new BizException("fin.error.statementsSentConfirmedCannotDeleted");
        }
    }

    /**
     * 对方确认或提出异议（有异议时须说明），记录意见和时间
     *
     * @param extra 同时设置的其他字段（如确认时填写的发票、确认人），可为 null
     */
    public void respond(BaseMapper<T> mapper, T st, boolean agree, String remark, Consumer<LambdaUpdateWrapper<T>> extra) {
        if (st.getStatus() != StatementStatus.SENT) {
            throw new BizException("fin.error.statementNotPendingConfirmation");
        }
        if (!agree && (remark == null || remark.isBlank())) {
            throw new BizException("fin.error.describeDispute");
        }
        StatusTransitions.change(mapper, idColumn, st.getId(), statusColumn, StatementStatus.SENT,
                agree ? StatementStatus.CONFIRMED : StatementStatus.DISPUTED, w -> {
                    w.set(partnerRemarkColumn, remark).set(confirmTimeColumn, LocalDateTime.now());
                    if (extra != null) {
                        extra.accept(w);
                    }
                }, CHANGED);
    }

    /**
     * 对方确认 / 提出异议后通知制单人（站内消息存库，用中文）
     *
     * @param agreedContent 确认时的消息内容（如对账金额），有异议时为异议说明
     * @param link          财务的对账单页面
     */
    public static void notifyCreator(MessageSender sender, FinStatementBase st, String partnerName, boolean agree, String remark,
                                     String agreedContent, String link) {
        if (st.getCreateBy() == null) {
            return;
        }
        sender.send(List.of(st.getCreateBy()), MessageSender.Message.of(agree ? MessageCategory.REMIND : MessageCategory.ALERT,
                partnerName + (agree ? " 已确认对账单 " : " 对对账单有异议 ") + st.getStatementNo(), // i18n-ignore
                agree ? agreedContent : remark, link));
    }
}
