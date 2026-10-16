package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.entity.QmsSupplierClaim;
import com.lemon.erp.modules.mes.event.SupplierClaimCancelledEvent;
import com.lemon.erp.modules.mes.event.SupplierClaimConfirmedEvent;
import com.lemon.erp.modules.mes.mapper.QmsNcrMapper;
import com.lemon.erp.modules.mes.mapper.QmsSupplierClaimMapper;
import com.lemon.erp.modules.mes.model.ClaimStatus;
import com.lemon.erp.modules.mes.model.NcrStatus;
import com.lemon.erp.modules.scm.service.ScmPortalNotifier;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 供应商质量索赔：可关联不合格品处理单；确认后财务生成红字应付单冲减应付（借 应付账款，贷 营业外收入），
 * 取消已确认的索赔时删除该应付单（已核销或已生成凭证的不能取消）
 */
@Service
@RequiredArgsConstructor
public class QmsClaimService {

    /** 索赔单号的编码规则标识 */
    public static final String DOC_NO = "SZ";

    private final QmsSupplierClaimMapper claimMapper;
    private final QmsNcrMapper ncrMapper;
    private final ScmSupplierService supplierService;
    private final DocNoService docNoService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScmPortalNotifier portalNotifier;

    public PageResult<QmsSupplierClaim> page(long current, long size, ClaimStatus status, Long supplierId, String keyword) {
        Page<QmsSupplierClaim> page = Page.of(current, size);
        claimMapper.selectPageList(page, status, supplierId, keyword, false);
        return PageResult.of(page);
    }

    /** 供应商门户：本供应商已确认、已取消的索赔 */
    public PageResult<QmsSupplierClaim> pageForSupplier(long current, long size, Long supplierId) {
        Page<QmsSupplierClaim> page = Page.of(current, size);
        claimMapper.selectPageList(page, null, supplierId, null, true);
        return PageResult.of(page);
    }

    public QmsSupplierClaim detail(Long id) {
        QmsSupplierClaim claim = id == null ? null : claimMapper.selectDetail(id);
        if (claim == null) {
            throw new BizException("索赔单不存在");
        }
        return claim;
    }

    @Transactional
    public Long create(QmsRequests.Claim req) {
        QmsSupplierClaim claim = new QmsSupplierClaim();
        claim.setClaimNo(docNoService.next(DOC_NO));
        apply(claim, req);
        claim.setStatus(ClaimStatus.DRAFT);
        claim.setCreateBy(SecurityUtils.getUserId());
        claimMapper.insert(claim);
        return claim.getId();
    }

    @Transactional
    public void update(Long id, QmsRequests.Claim req) {
        QmsSupplierClaim claim = requireStatus(id, ClaimStatus.DRAFT, "修改");
        apply(claim, req);
        claimMapper.updateById(claim);
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, ClaimStatus.DRAFT, "删除");
        claimMapper.deleteById(id);
    }

    /** 确认：财务生成红字应付单 */
    @Transactional
    public void confirm(Long id) {
        QmsSupplierClaim claim = requireStatus(id, ClaimStatus.DRAFT, "确认");
        transit(id, ClaimStatus.DRAFT, ClaimStatus.CONFIRMED);
        eventPublisher.publishEvent(new SupplierClaimConfirmedEvent(id, claim.getClaimNo(), claim.getSupplierId(), claim.getAmount(),
                claim.getClaimDate(), claim.getReason()));
        portalNotifier.notify(claim.getSupplierId(), "ALERT", "质量索赔：" + claim.getClaimNo(),
                "索赔金额 " + claim.getAmount().stripTrailingZeros().toPlainString() + "，原因：" + claim.getReason(), "/portal/claim");
    }

    /** 取消已确认的索赔：删除红字应付单 */
    @Transactional
    public void cancel(Long id) {
        QmsSupplierClaim claim = requireStatus(id, ClaimStatus.CONFIRMED, "取消");
        transit(id, ClaimStatus.CONFIRMED, ClaimStatus.CANCELLED);
        eventPublisher.publishEvent(new SupplierClaimCancelledEvent(id, claim.getClaimNo(), claim.getBillId()));
    }

    // ---------------------------------------------------------------- 内部

    private void apply(QmsSupplierClaim claim, QmsRequests.Claim req) {
        supplierService.get(req.supplierId());
        if (req.ncrId() != null) {
            QmsNcr ncr = ncrMapper.selectById(req.ncrId());
            if (ncr == null) {
                throw new BizException("不合格品处理单不存在");
            }
            if (ncr.getStatus() == NcrStatus.CANCELLED) {
                throw new BizException("不合格品处理单已取消");
            }
            if (ncr.getSupplierId() != null && !ncr.getSupplierId().equals(req.supplierId())) {
                throw new BizException("索赔供应商与不合格品处理单的供应商不一致");
            }
        }
        claim.setSupplierId(req.supplierId());
        claim.setNcrId(req.ncrId());
        claim.setClaimDate(req.claimDate());
        claim.setAmount(req.amount());
        claim.setReason(req.reason());
    }

    private void transit(Long id, ClaimStatus from, ClaimStatus to) {
        if (claimMapper.update(Wrappers.<QmsSupplierClaim>lambdaUpdate()
                .set(QmsSupplierClaim::getStatus, to)
                .set(to == ClaimStatus.CONFIRMED, QmsSupplierClaim::getConfirmTime, LocalDateTime.now())
                .eq(QmsSupplierClaim::getId, id).eq(QmsSupplierClaim::getStatus, from)) == 0) {
            throw new BizException("索赔单状态已变化，请刷新后重试");
        }
    }

    private QmsSupplierClaim requireStatus(Long id, ClaimStatus status, String action) {
        QmsSupplierClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw new BizException("索赔单不存在");
        }
        if (claim.getStatus() != status) {
            throw new BizException("索赔单当前状态不能" + action);
        }
        return claim;
    }
}
