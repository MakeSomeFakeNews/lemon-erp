package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.workflow.FlowApi;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.scm.dto.SrmRequests;
import com.lemon.erp.modules.scm.dto.SrmViews;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContract;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContractItem;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmPurchaseContractItemMapper;
import com.lemon.erp.modules.scm.mapper.ScmPurchaseContractMapper;
import com.lemon.erp.modules.scm.model.ContractStatus;
import com.lemon.erp.modules.scm.model.ContractType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lemon.erp.modules.scm.model.ScmConstants.CONTRACT_RULE;

/**
 * 采购合同 / 框架协议：草稿 → 提交（发布了审批流程时审批）→ 生效 → 关闭。生效且在期限内的合同可被采购订单引用，
 * 按合同价下单并校验数量、金额上限（见 {@link ScmContractUsageService}），详情显示各物料的执行进度
 */
@Service
@RequiredArgsConstructor
public class ScmPurchaseContractService {

    /** 合同编号的编码规则标识 */
    public static final String DOC_NO = "CGHT";
    public static final String PROCESS_KEY = "scm_contract";
    private static final String BUSINESS_KEY_PREFIX = PROCESS_KEY + ":";
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(13);

    private final ScmPurchaseContractMapper contractMapper;
    private final ScmPurchaseContractItemMapper itemMapper;
    private final ScmOrderMapper orderMapper;
    private final ScmSupplierService supplierService;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final FlowApi flowApi;

    @DataPermission(table = "scm_purchase_contract", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<ScmPurchaseContract> page(long current, long size, ContractStatus status, Long supplierId, String keyword) {
        Page<ScmPurchaseContract> page = Page.of(current, size);
        contractMapper.selectPageList(page, status, supplierId, keyword);
        return PageResult.of(page);
    }

    /** 详情：明细含已下单数量、金额和已入库数量，以及引用的订单 */
    public ScmPurchaseContract detail(Long id) {
        getAccessible(id);
        ScmPurchaseContract contract = contractMapper.selectDetail(id);
        Map<Long, SrmViews.ContractUsage> usage = contractMapper.selectUsage(id, null).stream()
                .collect(Collectors.toMap(SrmViews.ContractUsage::materialId, Function.identity()));
        List<ScmPurchaseContractItem> items = itemMapper.selectByContract(id);
        items.forEach(i -> {
            SrmViews.ContractUsage u = usage.get(i.getMaterialId());
            i.setOrderedQty(u == null ? BigDecimal.ZERO : u.orderedQty());
            i.setOrderedAmount(u == null ? BigDecimal.ZERO : u.orderedAmount());
            i.setReceivedQty(u == null ? BigDecimal.ZERO : u.receivedQty());
        });
        contract.setItems(items);
        return contract;
    }

    public List<ScmOrder> orders(Long id) {
        getAccessible(id);
        return contractMapper.selectOrders(id);
    }

    /** 采购订单可引用的合同：供应商在订单日期有效的生效合同，含明细（合同价） */
    public List<ScmPurchaseContract> active(Long supplierId, LocalDate date) {
        List<ScmPurchaseContract> list = contractMapper.selectActive(supplierId, date != null ? date : LocalDate.now());
        list.forEach(c -> c.setItems(itemMapper.selectByContract(c.getId())));
        return list;
    }

    @Transactional
    public Long create(SrmRequests.Contract req) {
        return create(req, null);
    }

    /** @param tenderId 由招标定标生成时为来源招标 */
    @Transactional
    public Long create(SrmRequests.Contract req, Long tenderId) {
        List<ScmPurchaseContractItem> items = validate(req);
        LoginUser user = SecurityUtils.getLoginUser();
        ScmPurchaseContract contract = new ScmPurchaseContract();
        contract.setContractNo(docNoService.next(DOC_NO));
        apply(contract, req);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setTenderId(tenderId);
        contract.setDeptId(user.deptId());
        contract.setCreateBy(user.userId());
        contractMapper.insert(contract);
        saveItems(contract.getId(), items);
        return contract.getId();
    }

    @Transactional
    public void update(Long id, SrmRequests.Contract req) {
        ScmPurchaseContract contract = requireStatus(id, ContractStatus.DRAFT, "scm.label.edit");
        List<ScmPurchaseContractItem> items = validate(req);
        apply(contract, req);
        contractMapper.updateById(contract);
        itemMapper.delete(Wrappers.<ScmPurchaseContractItem>lambdaQuery().eq(ScmPurchaseContractItem::getContractId, id));
        saveItems(id, items);
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, ContractStatus.DRAFT, "scm.label.delete");
        if (orderMapper.exists(Wrappers.<ScmOrder>lambdaQuery().eq(ScmOrder::getContractId, id))) {
            throw new BizException("scm.error.contractReferencedPurchaseOrdersCannotDeleted");
        }
        itemMapper.delete(Wrappers.<ScmPurchaseContractItem>lambdaQuery().eq(ScmPurchaseContractItem::getContractId, id));
        contractMapper.deleteById(id);
    }

    /**
     * 提交：发布了审批流程时进入审批，否则直接生效
     *
     * @return 是否进入审批
     */
    @Transactional
    public boolean submit(Long id) {
        ScmPurchaseContract contract = requireStatus(id, ContractStatus.DRAFT, "scm.label.submit");
        if (!flowApi.isAvailable(PROCESS_KEY)) {
            activate(id, ContractStatus.DRAFT);
            return false;
        }
        transit(id, ContractStatus.DRAFT, ContractStatus.APPROVING);
        String instanceId = flowApi.start(PROCESS_KEY, BUSINESS_KEY_PREFIX + id, Map.of(
                "amount", contractAmount(contract, itemMapper.selectByContract(id)).doubleValue(),
                "contractType", contract.getContractType().name(),
                "contractNo", contract.getContractNo()));
        contractMapper.update(Wrappers.<ScmPurchaseContract>lambdaUpdate().set(ScmPurchaseContract::getFlowInstanceId, instanceId)
                .eq(ScmPurchaseContract::getId, id));
        return true;
    }

    /** 审批结束：通过 → 生效；拒绝、撤回 → 草稿 */
    @EventListener
    public void onFlowFinished(FlowFinishedEvent e) {
        if (e.businessKey() == null || !e.businessKey().startsWith(BUSINESS_KEY_PREFIX)) {
            return;
        }
        Long id = Long.valueOf(e.businessKey().substring(BUSINESS_KEY_PREFIX.length()));
        if (e.status() == FlowStatus.APPROVED) {
            contractMapper.update(Wrappers.<ScmPurchaseContract>lambdaUpdate()
                    .set(ScmPurchaseContract::getStatus, ContractStatus.ACTIVE)
                    .set(ScmPurchaseContract::getApproveTime, LocalDateTime.now())
                    .eq(ScmPurchaseContract::getId, id).eq(ScmPurchaseContract::getStatus, ContractStatus.APPROVING));
        } else {
            contractMapper.transit(id, ContractStatus.APPROVING, ContractStatus.DRAFT);
        }
    }

    /** 关闭：不再允许新订单引用，已下单的订单不受影响 */
    @Transactional
    public void close(Long id, String reason) {
        requireStatus(id, ContractStatus.ACTIVE, "scm.label.close");
        if (contractMapper.update(Wrappers.<ScmPurchaseContract>lambdaUpdate()
                .set(ScmPurchaseContract::getStatus, ContractStatus.CLOSED)
                .set(ScmPurchaseContract::getCloseReason, reason)
                .eq(ScmPurchaseContract::getId, id).eq(ScmPurchaseContract::getStatus, ContractStatus.ACTIVE)) == 0) {
            throw new BizException("scm.error.contractStatusHasChangedRefreshTry");
        }
    }

    // ---------------------------------------------------------------- 内部

    /** 合同金额：有金额上限取上限，否则为有数量上限的明细 数量 × 单价 合计 */
    private static BigDecimal contractAmount(ScmPurchaseContract contract, List<ScmPurchaseContractItem> items) {
        if (contract.getAmountLimit() != null) {
            return contract.getAmountLimit();
        }
        return items.stream().filter(i -> i.getQtyLimit() != null).map(i -> i.getQtyLimit().multiply(i.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ScmPurchaseContractItem> validate(SrmRequests.Contract req) {
        ScmSupplier supplier = supplierService.get(req.supplierId());
        if (!Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BizException("scm.error.supplierDisabled", supplier.getName());
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BizException("scm.error.expiryDateCannotEarlierEffectiveDate");
        }
        if (req.items().stream().map(SrmRequests.ContractItem::materialId).distinct().count() < req.items().size()) {
            throw new BizException("scm.error.duplicateContractMaterial");
        }
        if (req.contractType() == ContractType.FIXED && req.items().stream().anyMatch(i -> i.qtyLimit() == null)) {
            throw new BizException("scm.error.fixedQuantityContractsRequireQuantityEach");
        }
        materialService.getEnabled(req.items().stream().map(SrmRequests.ContractItem::materialId).toList());
        return req.items().stream().map(r -> {
            ScmPurchaseContractItem item = new ScmPurchaseContractItem();
            item.setMaterialId(r.materialId());
            item.setPrice(r.price());
            item.setTaxRate(r.taxRate() == null ? DEFAULT_TAX_RATE : r.taxRate());
            item.setQtyLimit(r.qtyLimit());
            item.setRemark(r.remark());
            return item;
        }).toList();
    }

    private static void apply(ScmPurchaseContract contract, SrmRequests.Contract req) {
        contract.setName(req.name().trim());
        contract.setSupplierId(req.supplierId());
        contract.setContractType(req.contractType());
        contract.setSignDate(req.signDate());
        contract.setStartDate(req.startDate());
        contract.setEndDate(req.endDate());
        contract.setAmountLimit(req.amountLimit());
        contract.setPaymentDays(req.paymentDays());
        contract.setPaymentTerms(req.paymentTerms());
        contract.setRemark(req.remark());
    }

    private void saveItems(Long contractId, List<ScmPurchaseContractItem> items) {
        items.forEach(i -> i.setContractId(contractId));
        itemMapper.insertBatch(items);
    }

    private void activate(Long id, ContractStatus from) {
        if (contractMapper.update(Wrappers.<ScmPurchaseContract>lambdaUpdate()
                .set(ScmPurchaseContract::getStatus, ContractStatus.ACTIVE)
                .set(ScmPurchaseContract::getApproveTime, LocalDateTime.now())
                .eq(ScmPurchaseContract::getId, id).eq(ScmPurchaseContract::getStatus, from)) == 0) {
            throw new BizException("scm.error.contractStatusHasChangedRefreshTry");
        }
    }

    private void transit(Long id, ContractStatus from, ContractStatus to) {
        if (contractMapper.transit(id, from, to) == 0) {
            throw new BizException("scm.error.contractStatusHasChangedRefreshTry");
        }
    }

    private ScmPurchaseContract getAccessible(Long id) {
        Supplier<ScmPurchaseContract> loader = () -> contractMapper.selectById(id);
        ScmPurchaseContract contract = SecurityUtils.getDataScope().all() ? loader.get() : DataPermissionContext.execute(CONTRACT_RULE, loader);
        if (contract == null) {
            throw new BizException("scm.error.purchaseContractDoesNotExistAccess");
        }
        return contract;
    }

    private ScmPurchaseContract requireStatus(Long id, ContractStatus status, String actionKey) {
        ScmPurchaseContract contract = getAccessible(id);
        if (contract.getStatus() != status) {
            throw new BizException("scm.error.operationNotAllowedContractSCurrent", Messages.get(actionKey));
        }
        return contract;
    }
}
