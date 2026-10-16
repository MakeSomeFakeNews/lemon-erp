package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.entity.ScmBid;
import com.lemon.erp.modules.scm.entity.ScmBidItem;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.entity.ScmTender;
import com.lemon.erp.modules.scm.entity.ScmTenderItem;
import com.lemon.erp.modules.scm.entity.ScmTenderSupplier;
import com.lemon.erp.modules.scm.mapper.ScmBidMapper;
import com.lemon.erp.modules.scm.mapper.ScmTenderMapper;
import com.lemon.erp.modules.scm.model.AwardTarget;
import com.lemon.erp.modules.scm.model.ContractType;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TenderStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.TENDER_RULE;
import static com.lemon.erp.modules.scm.model.ScmConstants.amount;

/**
 * 招标：草稿 → 发布（通知邀请的供应商）→ 供应商在门户投标（截止前可修改，采购也可代录线下标书）→
 * 截止后开标（开标前看不到报价）→ 定标，生成采购合同或采购订单（草稿）
 */
@Service
@RequiredArgsConstructor
public class ScmTenderService {

    /** 招标编号的编码规则标识 */
    public static final String DOC_NO = "ZB";
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    private final ScmTenderMapper tenderMapper;
    private final ScmBidMapper bidMapper;
    private final ScmSupplierService supplierService;
    private final WmsMaterialService materialService;
    private final ScmPurchaseContractService contractService;
    private final ScmOrderService orderService;
    private final ScmPortalNotifier portalNotifier;
    private final DocNoService docNoService;

    @DataPermission(table = "scm_tender", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmTender> page(long current, long size, TenderStatus status, String keyword) {
        Page<ScmTender> page = Page.of(current, size);
        tenderMapper.selectPageList(page, status, keyword);
        return PageResult.of(page);
    }

    /** 详情：开标前投标只显示供应商和提交时间，不显示报价 */
    public ScmTender detail(Long id) {
        getAccessible(id);
        ScmTender tender = tenderMapper.selectDetail(id);
        tender.setItems(tenderMapper.selectItems(id));
        tender.setSuppliers(tenderMapper.selectSuppliers(id));
        List<ScmBid> bids = bidMapper.selectByTender(id);
        if (opened(tender)) {
            attachItems(bids);
        } else {
            bids.forEach(b -> b.setTotalAmount(null));
        }
        tender.setBids(bids);
        return tender;
    }

    @Transactional
    public Long create(SrmRequests.Tender req) {
        validate(req);
        LoginUser user = SecurityUtils.getLoginUser();
        ScmTender tender = new ScmTender();
        tender.setTenderNo(docNoService.next(DOC_NO));
        apply(tender, req);
        tender.setStatus(TenderStatus.DRAFT);
        tender.setDeptId(user.deptId());
        tender.setCreateBy(user.userId());
        tenderMapper.insert(tender);
        saveLines(tender.getId(), req);
        return tender.getId();
    }

    @Transactional
    public void update(Long id, SrmRequests.Tender req) {
        ScmTender tender = requireStatus(id, TenderStatus.DRAFT, "修改");
        validate(req);
        apply(tender, req);
        tenderMapper.updateById(tender);
        deleteLines(id);
        saveLines(id, req);
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, TenderStatus.DRAFT, "删除");
        deleteLines(id);
        tenderMapper.deleteById(id);
    }

    /** 发布：通知邀请的供应商在门户投标 */
    @Transactional
    public void publish(Long id) {
        ScmTender tender = requireStatus(id, TenderStatus.DRAFT, "发布");
        if (!tender.getDeadline().isAfter(LocalDateTime.now())) {
            throw new BizException("投标截止时间已过，请修改后再发布");
        }
        transit(id, TenderStatus.DRAFT, TenderStatus.BIDDING);
        for (ScmTenderSupplier s : tenderMapper.selectSuppliers(id)) {
            portalNotifier.notify(s.getSupplierId(), "REMIND", "招标邀请：" + tender.getTitle(),
                    "招标编号 " + tender.getTenderNo() + "，请在 " + tender.getDeadline().toString().replace('T', ' ') + " 前投标", "/portal/tender");
        }
    }

    @Transactional
    public void cancel(Long id) {
        ScmTender tender = getAccessible(id);
        if (tender.getStatus() == TenderStatus.AWARDED || tender.getStatus() == TenderStatus.CANCELLED) {
            throw new BizException("招标已定标或已取消");
        }
        transit(id, tender.getStatus(), TenderStatus.CANCELLED);
    }

    /**
     * 投标：截止前可以修改；报价须覆盖全部招标物料
     *
     * @param supplierId 投标供应商（门户为当前账号的供应商，代录时由采购选择）
     */
    @Transactional
    public void saveBid(Long tenderId, Long supplierId, SrmRequests.Bid req) {
        ScmTender tender = tenderMapper.selectById(tenderId);
        if (tender == null || tender.getStatus() != TenderStatus.BIDDING) {
            throw new BizException("招标不在投标中");
        }
        if (!LocalDateTime.now().isBefore(tender.getDeadline())) {
            throw new BizException("已过投标截止时间");
        }
        if (tenderMapper.selectSuppliers(tenderId).stream().noneMatch(s -> s.getSupplierId().equals(supplierId))) {
            throw new BizException("该供应商没有被邀请参加此次招标");
        }
        Map<Long, ScmTenderItem> items = tenderMapper.selectItems(tenderId).stream()
                .collect(Collectors.toMap(ScmTenderItem::getId, Function.identity()));
        Set<Long> quoted = req.items().stream().map(SrmRequests.BidItem::tenderItemId).collect(Collectors.toSet());
        if (quoted.size() != req.items().size() || !quoted.equals(items.keySet())) {
            throw new BizException("请为每个招标物料报价");
        }
        BigDecimal total = req.items().stream().map(r -> amount(items.get(r.tenderItemId()).getQty(), r.price()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ScmBid bid = bidMapper.selectOne(Wrappers.<ScmBid>lambdaQuery().eq(ScmBid::getTenderId, tenderId).eq(ScmBid::getSupplierId, supplierId));
        if (bid == null) {
            bid = new ScmBid();
            bid.setTenderId(tenderId);
            bid.setSupplierId(supplierId);
        }
        bid.setTotalAmount(total);
        bid.setDeliveryDays(req.deliveryDays());
        bid.setRemark(req.remark());
        bid.setSubmitBy(SecurityUtils.getUserId());
        bid.setSubmitTime(LocalDateTime.now());
        if (bid.getId() == null) {
            bidMapper.insert(bid);
        } else {
            bidMapper.updateById(bid);
            bidMapper.deleteItems(bid.getId());
        }
        Long bidId = bid.getId();
        bidMapper.insertItems(req.items().stream().map(r -> {
            ScmBidItem item = new ScmBidItem();
            item.setBidId(bidId);
            item.setTenderItemId(r.tenderItemId());
            item.setPrice(r.price());
            item.setTaxRate(r.taxRate() == null ? DEFAULT_TAX_RATE : r.taxRate());
            item.setRemark(r.remark());
            return item;
        }).toList());
    }

    /** 开标：截止后，或邀请的供应商都已投标时可以提前开标 */
    @Transactional
    public void open(Long id) {
        ScmTender tender = requireStatus(id, TenderStatus.BIDDING, "开标");
        boolean allBid = tenderMapper.selectSuppliers(id).stream().allMatch(s -> Boolean.TRUE.equals(s.getBid()));
        if (LocalDateTime.now().isBefore(tender.getDeadline()) && !allBid) {
            throw new BizException("投标截止前不能开标（邀请的供应商都已投标时可以提前开标）");
        }
        if (tenderMapper.update(Wrappers.<ScmTender>lambdaUpdate()
                .set(ScmTender::getStatus, TenderStatus.OPENED).set(ScmTender::getOpenTime, LocalDateTime.now())
                .eq(ScmTender::getId, id).eq(ScmTender::getStatus, TenderStatus.BIDDING)) == 0) {
            throw new BizException("招标状态已变化，请刷新后重试");
        }
    }

    /** 定标：选择中标供应商，按其报价生成采购合同或采购订单（草稿） */
    @Transactional
    public void award(Long id, SrmRequests.Award req) {
        ScmTender tender = requireStatus(id, TenderStatus.OPENED, "定标");
        ScmBid bid = bidMapper.selectOne(Wrappers.<ScmBid>lambdaQuery().eq(ScmBid::getTenderId, id).eq(ScmBid::getSupplierId, req.supplierId()));
        if (bid == null) {
            throw new BizException("该供应商没有投标");
        }
        attachItems(List.of(bid));
        Map<Long, ScmBidItem> prices = bid.getItems().stream().collect(Collectors.toMap(ScmBidItem::getTenderItemId, Function.identity()));
        List<ScmTenderItem> items = tenderMapper.selectItems(id);
        Long contractId = null;
        Long orderId = null;
        if (req.target() == AwardTarget.CONTRACT) {
            LocalDate start = req.startDate() != null ? req.startDate() : LocalDate.now();
            LocalDate end = req.endDate() != null ? req.endDate() : start.plusYears(1).minusDays(1);
            contractId = contractService.create(new SrmRequests.Contract(tender.getTitle(), req.supplierId(), ContractType.FIXED,
                    LocalDate.now(), start, end, null, null, null, "招标 " + tender.getTenderNo() + " 定标生成",
                    items.stream().map(i -> new SrmRequests.ContractItem(i.getMaterialId(), prices.get(i.getId()).getPrice(),
                            prices.get(i.getId()).getTaxRate(), i.getQty(), i.getRemark())).toList()), id);
        } else if (req.target() == AwardTarget.ORDER) {
            if (req.warehouseId() == null) {
                throw new BizException("请选择采购订单的收货仓库");
            }
            LocalDate leadDate = LocalDate.now().plusDays(bid.getDeliveryDays() == null ? 0 : bid.getDeliveryDays());
            orderId = orderService.createDraft(OrderKind.PURCHASE, new OrderRequest(req.supplierId(), LocalDate.now(), null, req.warehouseId(),
                    "招标 " + tender.getTenderNo() + " 定标生成",
                    items.stream().map(i -> new OrderItemRequest(i.getMaterialId(), i.getQty(), prices.get(i.getId()).getPrice(),
                            prices.get(i.getId()).getTaxRate(), i.getRequiredDate() != null ? i.getRequiredDate() : leadDate, i.getRemark()))
                            .toList()), "TENDER");
        }
        if (tenderMapper.update(Wrappers.<ScmTender>lambdaUpdate()
                .set(ScmTender::getStatus, TenderStatus.AWARDED)
                .set(ScmTender::getAwardSupplierId, req.supplierId())
                .set(ScmTender::getAwardReason, req.reason())
                .set(ScmTender::getContractId, contractId)
                .set(ScmTender::getOrderId, orderId)
                .eq(ScmTender::getId, id).eq(ScmTender::getStatus, TenderStatus.OPENED)) == 0) {
            throw new BizException("招标状态已变化，请刷新后重试");
        }
        portalNotifier.notify(req.supplierId(), "REMIND", "中标通知：" + tender.getTitle(),
                "您在招标 " + tender.getTenderNo() + " 中中标，采购将与您签订合同或下达订单", "/portal/tender");
    }

    // ---------------------------------------------------------------- 供应商门户

    public PageResult<ScmTender> pageForSupplier(long current, long size, Long supplierId) {
        Page<ScmTender> page = Page.of(current, size);
        tenderMapper.selectForSupplier(page, supplierId);
        return PageResult.of(page);
    }

    /** 门户详情：招标信息和本供应商的投标，不显示其他供应商 */
    public ScmTender detailForSupplier(Long id, Long supplierId) {
        ScmTender tender = tenderMapper.selectById(id);
        if (tender == null || tender.getStatus() == TenderStatus.DRAFT
                || tenderMapper.selectSuppliers(id).stream().noneMatch(s -> s.getSupplierId().equals(supplierId))) {
            throw new BizException("招标不存在");
        }
        ScmTender view = new ScmTender();
        view.setId(tender.getId());
        view.setTenderNo(tender.getTenderNo());
        view.setTitle(tender.getTitle());
        view.setDeadline(tender.getDeadline());
        view.setStatus(tender.getStatus());
        view.setRemark(tender.getRemark());
        view.setCreateTime(tender.getCreateTime());
        if (tender.getStatus() == TenderStatus.AWARDED) {
            view.setAwardSupplierId(supplierId.equals(tender.getAwardSupplierId()) ? supplierId : null);
        }
        view.setItems(tenderMapper.selectItems(id));
        List<ScmBid> own = bidMapper.selectByTender(id).stream().filter(b -> b.getSupplierId().equals(supplierId)).toList();
        attachItems(own);
        view.setBids(own);
        return view;
    }

    // ---------------------------------------------------------------- 内部

    private static boolean opened(ScmTender tender) {
        return tender.getStatus() == TenderStatus.OPENED || tender.getStatus() == TenderStatus.AWARDED;
    }

    private void attachItems(List<ScmBid> bids) {
        if (bids.isEmpty()) {
            return;
        }
        Map<Long, List<ScmBidItem>> items = bidMapper.selectItems(bids.stream().map(ScmBid::getId).toList()).stream()
                .collect(Collectors.groupingBy(ScmBidItem::getBidId));
        bids.forEach(b -> b.setItems(items.getOrDefault(b.getId(), List.of())));
    }

    private void validate(SrmRequests.Tender req) {
        if (!req.deadline().isAfter(LocalDateTime.now())) {
            throw new BizException("投标截止时间须晚于当前时间");
        }
        if (req.items().stream().map(SrmRequests.TenderItem::materialId).distinct().count() < req.items().size()) {
            throw new BizException("招标物料重复");
        }
        materialService.getEnabled(req.items().stream().map(SrmRequests.TenderItem::materialId).toList());
        if (req.supplierIds().stream().distinct().count() < req.supplierIds().size()) {
            throw new BizException("邀请的供应商重复");
        }
        for (Long supplierId : req.supplierIds()) {
            ScmSupplier supplier = supplierService.get(supplierId);
            if (!Integer.valueOf(1).equals(supplier.getStatus())) {
                throw new BizException("供应商「" + supplier.getName() + "」已停用");
            }
        }
    }

    private static void apply(ScmTender tender, SrmRequests.Tender req) {
        tender.setTitle(req.title().trim());
        tender.setDeadline(req.deadline().withNano(0));
        tender.setRemark(req.remark());
    }

    private void saveLines(Long tenderId, SrmRequests.Tender req) {
        tenderMapper.insertItems(req.items().stream().map(r -> {
            ScmTenderItem item = new ScmTenderItem();
            item.setTenderId(tenderId);
            item.setMaterialId(r.materialId());
            item.setQty(r.qty());
            item.setRequiredDate(r.requiredDate());
            item.setRemark(r.remark());
            return item;
        }).toList());
        tenderMapper.insertSuppliers(tenderId, req.supplierIds());
    }

    private void deleteLines(Long tenderId) {
        tenderMapper.deleteItems(tenderId);
        tenderMapper.deleteSuppliers(tenderId);
    }

    private void transit(Long id, TenderStatus from, TenderStatus to) {
        if (tenderMapper.transit(id, from, to) == 0) {
            throw new BizException("招标状态已变化，请刷新后重试");
        }
    }

    private ScmTender getAccessible(Long id) {
        Supplier<ScmTender> loader = () -> tenderMapper.selectById(id);
        ScmTender tender = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(TENDER_RULE, loader);
        if (tender == null) {
            throw new BizException("招标不存在或没有权限");
        }
        return tender;
    }

    private ScmTender requireStatus(Long id, TenderStatus status, String action) {
        ScmTender tender = getAccessible(id);
        if (tender.getStatus() != status) {
            throw new BizException("招标当前状态不能" + action);
        }
        return tender;
    }
}
