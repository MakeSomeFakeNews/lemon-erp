package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.SearchKeyTable;
import com.lemon.erp.framework.data.SearchKeys;
import com.lemon.erp.framework.quality.SupplierQualityProvider;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.scm.dto.CarrierOptions;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.dto.SupplierKpi;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.entity.ScmSupplierEval;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierEvalMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierPriceMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.SupplierLevel;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScmSupplierService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("code", "供应商编码", "name", "供应商名称", "contact", "联系人", "phone", "联系电话", "email", "邮箱", // i18n-ignore
            "address", "地址", "taxNo", "纳税人识别号", "bankName", "开户行", "bankAccount", "银行账号", "paymentDays", "账期", "logistics", "物流公司", "routes", "营运路线", // i18n-ignore
            "customerId", "关联客户", "remark", "备注"); // i18n-ignore

    /** 下拉搜索每页最多返回的条数 */
    public static final int OPTION_LIMIT = 50;
    /** logistics 取值：物流公司 */
    private static final int LOGISTICS = 1;

    private final ScmSupplierMapper supplierMapper;
    private final ScmCustomerMapper customerMapper;
    private final DocNoService docNoService;
    private final ScmSupplierPriceMapper priceMapper;
    private final ScmSupplierEvalMapper evalMapper;
    private final ScmOrderMapper orderMapper;
    private final ReferenceGuard referenceGuard;
    private final ChangeLogger changeLogger;
    private final SupplierQualityProvider qualityProvider;
    private final SearchKeyTable<ScmSupplier> searchKeys;

    /**
     * @param keyword   编码、名称、联系人、电话、拼音首字母或去掉分隔符的缩写，规则见 {@link SearchKeys}
     * @param logistics 1 只查物流公司，0 只查其他供应商，为空不限
     */
    public PageResult<ScmSupplier> page(long current, long size, String keyword, SupplierLevel level, EnableStatus status, Integer logistics) {
        Page<ScmSupplier> page = supplierMapper.selectPage(Page.of(current, size), Wrappers.<ScmSupplier>lambdaQuery()
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .eq(level != null, ScmSupplier::getLevel, level)
                .eq(logistics != null, ScmSupplier::getLogistics, logistics)
                .eq(status != null, ScmSupplier::getStatus, status)
                .orderByAsc(ScmSupplier::getCode));
        fillNames(page.getRecords());
        return PageResult.of(page);
    }

    /** 关联客户、已合并到的供应商的显示名（编码 名称），一次查询 */
    private void fillNames(List<ScmSupplier> rows) {
        List<Long> customerIds = rows.stream().map(ScmSupplier::getCustomerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> customers = customerIds.isEmpty() ? Collections.<Long, String>emptyMap() : customerMapper.selectByIds(customerIds).stream()
                .collect(Collectors.toMap(ScmCustomer::getId, c -> c.getCode() + " " + c.getName()));
        List<Long> mergedIds = rows.stream().map(ScmSupplier::getMergedTo).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> merged = mergedIds.isEmpty() ? Collections.<Long, String>emptyMap() : supplierMapper.selectByIds(mergedIds).stream()
                .collect(Collectors.toMap(ScmSupplier::getId, x -> x.getCode() + " " + x.getName()));
        rows.forEach(r -> {
            r.setCustomerName(customers.get(r.getCustomerId()));
            r.setMergedToName(merged.get(r.getMergedTo()));
        });
    }

    public ScmSupplier get(Long id) {
        ScmSupplier supplier = id == null ? null : supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BizException("scm.error.supplierDoesNotExist");
        }
        return supplier;
    }

    /** 详情（编辑表单）：带关联客户、已合并到的供应商的名称 */
    public ScmSupplier detail(Long id) {
        ScmSupplier supplier = get(id);
        fillNames(List.of(supplier));
        return supplier;
    }

    /**
     * 关联该客户的供应商（同一单位既是客户又是供应商），没有时返回 null；供财务模块挂接应付、应收应付对冲使用
     */
    public ScmSupplier linkedSupplier(Long customerId) {
        return customerId == null ? null : supplierMapper.selectOne(Wrappers.<ScmSupplier>lambdaQuery()
                .select(ScmSupplier::getId, ScmSupplier::getCode, ScmSupplier::getName, ScmSupplier::getCustomerId, ScmSupplier::getStatus)
                .eq(ScmSupplier::getCustomerId, customerId));
    }

    /** 关联客户的校验：客户存在、未被合并，且没有被其他供应商关联（一对一） */
    private void checkLinkedCustomer(Long customerId, Long supplierId) {
        if (customerId == null) {
            return;
        }
        ScmCustomer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException("scm.error.customerDoesNotExist");
        }
        if (customer.getMergedTo() != null) {
            throw new BizException("scm.error.linkedCustomerMerged", customer.getName());
        }
        ScmSupplier other = linkedSupplier(customerId);
        if (other != null && !other.getId().equals(supplierId)) {
            throw new BizException("scm.error.customerAlreadyLinked", customer.getName(), other.getCode() + " " + other.getName());
        }
    }

    /**
     * 按税号精确匹配供应商（不限状态），供财务模块的发票导入等识别往来单位
     *
     * @return 第一个匹配的供应商 ID，没有时返回 null
     */
    public Long findIdByTaxNo(String taxNo) {
        return firstId(supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery().eq(ScmSupplier::getTaxNo, taxNo)));
    }

    /**
     * 按名称精确匹配供应商（不限状态），供财务模块的发票导入等识别往来单位
     *
     * @return 第一个匹配的供应商 ID，没有时返回 null
     */
    public Long findIdByName(String name) {
        return firstId(supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery().eq(ScmSupplier::getName, name)));
    }

    /**
     * 按编码或名称精确匹配供应商（不限状态），供财务模块的期初导入等按文字识别往来单位。编码完全一致的优先
     *
     * @return 供应商 ID，没有时返回 null
     */
    public Long findIdByCodeOrName(String key) {
        List<ScmSupplier> list = supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery()
                .eq(ScmSupplier::getCode, key).or().eq(ScmSupplier::getName, key));
        if (list.isEmpty()) {
            return null;
        }
        return list.stream().filter(x -> key.equals(x.getCode())).findFirst().orElse(list.get(0)).getId();
    }

    private static Long firstId(List<ScmSupplier> list) {
        return list.isEmpty() ? null : list.get(0).getId();
    }

    /** 启用的供应商，按关键字搜索，最多 {@value #OPTION_LIMIT} 条 */
    public List<PartnerOption> options(String keyword) {
        return options(keyword, false, 1, OPTION_LIMIT);
    }

    /**
     * 启用的供应商下拉：按关键字（规则见 {@link SearchKeys}）分页搜索，按编码排序，不查总数
     *
     * @param size 每页条数，最多 {@value #OPTION_LIMIT}
     */
    public List<PartnerOption> options(String keyword, long current, long size) {
        return options(keyword, false, current, size);
    }

    /**
     * 承运商下拉：启用的物流公司（{@code logistics = 1}），按编码或名称搜索；还没有标记任何物流公司时回退到全部启用的供应商
     */
    public CarrierOptions carrierOptions(String keyword, long current, long size) {
        boolean marked = supplierMapper.exists(Wrappers.<ScmSupplier>lambdaQuery()
                .eq(ScmSupplier::getStatus, EnableStatus.ENABLED).eq(ScmSupplier::getLogistics, LOGISTICS));
        return new CarrierOptions(marked, options(keyword, marked, current, size));
    }

    /** 第一页中编码与关键字完全一致的供应商排在最前 */
    private List<PartnerOption> options(String keyword, boolean logisticsOnly, long current, long size) {
        List<ScmSupplier> rows = supplierMapper.selectPage(Page.of(Math.max(current, 1), Math.min(Math.max(size, 1), OPTION_LIMIT), false),
                Wrappers.<ScmSupplier>lambdaQuery()
                        .eq(ScmSupplier::getStatus, EnableStatus.ENABLED)
                        .eq(logisticsOnly, ScmSupplier::getLogistics, LOGISTICS)
                        .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                        .orderByAsc(ScmSupplier::getCode)).getRecords();
        if (StringUtils.hasText(keyword) && current <= 1) {
            ScmSupplier exact = supplierMapper.selectOne(Wrappers.<ScmSupplier>lambdaQuery()
                    .eq(ScmSupplier::getStatus, EnableStatus.ENABLED)
                    .eq(logisticsOnly, ScmSupplier::getLogistics, LOGISTICS)
                    .eq(ScmSupplier::getCode, keyword.trim()));
            rows = SearchKeys.exactFirst(rows, exact, ScmSupplier::getId);
        }
        return rows.stream().map(ScmSupplierService::toOption).toList();
    }

    /** 按 ID 取供应商（含停用的，最多 {@value #OPTION_LIMIT} 个），下拉回显已选的值 */
    public List<PartnerOption> optionsByIds(List<Long> ids) {
        return ids.isEmpty() ? List.of() : supplierMapper.selectByIds(ids.stream().distinct().limit(OPTION_LIMIT).toList())
                .stream().map(ScmSupplierService::toOption).toList();
    }

    private static PartnerOption toOption(ScmSupplier s) {
        return new PartnerOption(s.getId(), s.getCode(), s.getName(), s.getContact(), s.getPhone(), s.getAddress(),
                s.getLevel() == null ? null : s.getLevel().name());
    }

    /**
     * 数据整理的候选记录：启用的供应商，按 ID 升序分页，只查分析需要的列（不含电话、银行账号等）
     *
     * @param keyword 编码或名称，模糊匹配
     */
    public PageResult<ScmSupplier> pageForCleanse(long current, long size, String keyword) {
        Page<ScmSupplier> page = supplierMapper.selectPage(Page.of(current, size), Wrappers.<ScmSupplier>lambdaQuery()
                .select(ScmSupplier::getId, ScmSupplier::getCode, ScmSupplier::getName, ScmSupplier::getContact, ScmSupplier::getAddress, ScmSupplier::getTaxNo)
                .eq(ScmSupplier::getStatus, EnableStatus.ENABLED)
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .orderByAsc(ScmSupplier::getId));
        return PageResult.of(page);
    }

    /** 关键字匹配：编码、名称、联系人、电话包含关键字，或检索列包含规范化的关键字 */
    private static Consumer<LambdaQueryWrapper<ScmSupplier>> keywordMatch(String keyword) {
        return SearchKeys.matches(keyword, ScmSupplier::getSearchKey,
                ScmSupplier::getCode, ScmSupplier::getName, ScmSupplier::getContact, ScmSupplier::getPhone);
    }

    /** 是否有其他启用的供应商使用该名称 */
    public boolean existsEnabledName(String name, Long excludeId) {
        return supplierMapper.exists(Wrappers.<ScmSupplier>lambdaQuery()
                .eq(ScmSupplier::getName, name)
                .eq(ScmSupplier::getStatus, EnableStatus.ENABLED)
                .ne(excludeId != null, ScmSupplier::getId, excludeId));
    }

    @Transactional
    public void create(ScmSupplier supplier) {
        if (!StringUtils.hasText(supplier.getCode())) {
            supplier.setCode(docNoService.next(ScmDocNos.SUPPLIER));
        }
        checkCodeUnique(supplier.getCode(), null);
        supplier.setId(null);
        if (supplier.getStatus() == null) supplier.setStatus(EnableStatus.ENABLED);
        if (supplier.getPaymentDays() == null) supplier.setPaymentDays(0);
        supplier.setLogistics(Integer.valueOf(LOGISTICS).equals(supplier.getLogistics()) ? LOGISTICS : 0);
        checkLinkedCustomer(supplier.getCustomerId(), null);
        supplierMapper.insert(supplier);
    }

    @Transactional
    public void update(Long id, ScmSupplier supplier) {
        Object before = supplierMapper.selectById(id);
        get(id);
        checkCodeUnique(supplier.getCode(), id);
        supplier.setId(id);
        supplier.setStatus(null);
        if (supplier.getPaymentDays() == null) supplier.setPaymentDays(0);
        if (supplier.getLogistics() != null) supplier.setLogistics(supplier.getLogistics() == LOGISTICS ? LOGISTICS : 0);
        checkLinkedCustomer(supplier.getCustomerId(), id);
        supplierMapper.updateById(supplier);
        // 关联客户可以清空：单独按请求值更新（updateById 不更新为空的字段）
        supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate().set(ScmSupplier::getCustomerId, supplier.getCustomerId()).eq(ScmSupplier::getId, id));
        ScmSupplier after = supplierMapper.selectById(id);
        searchKeys.refresh(after);
        changeLogger.log("scm_supplier", id, before, after, CHANGE_FIELDS);
    }

    public void changeStatus(Long id, EnableStatus status) {
        ScmSupplier old = detail(id);
        if (old.getMergedTo() != null && status == EnableStatus.ENABLED) {
            throw new BizException("scm.error.mergedCannotEnable", old.getName(), old.getMergedToName());
        }
        ScmSupplier entity = new ScmSupplier();
        entity.setId(id);
        entity.setStatus(status);
        supplierMapper.updateById(entity);
    }

    /** 有订单的供应商不能删除（可以停用）；删除时一并删除供货价格和评估记录 */
    @Transactional
    public void delete(List<Long> ids) {
        for (ScmSupplier supplier : supplierMapper.selectByIds(ids)) {
            if (orderMapper.exists(Wrappers.<ScmOrder>lambdaQuery()
                    .eq(ScmOrder::getKind, OrderKind.PURCHASE).eq(ScmOrder::getPartnerId, supplier.getId()))) {
                throw new BizException("scm.error.supplierHasPurchaseOrdersCannotDeleted", supplier.getName());
            }
            referenceGuard.checkDeletable(RefType.SUPPLIER, supplier.getId(), Messages.get("scm.text.supplier", supplier.getName()));
        }
        priceMapper.delete(Wrappers.<ScmSupplierPrice>lambdaQuery().in(ScmSupplierPrice::getSupplierId, ids));
        evalMapper.delete(Wrappers.<ScmSupplierEval>lambdaQuery().in(ScmSupplierEval::getSupplierId, ids));
        supplierMapper.deleteByIds(ids);
    }

    /** 供应商绩效 */
    public PageResult<SupplierKpi> kpi(long current, long size, String keyword, LocalDate begin, LocalDate end) {
        Page<SupplierKpi> page = Page.of(current, size);
        supplierMapper.selectKpi(page, keyword, begin, end);
        Map<Long, SupplierQualityProvider.QualityStat> stats = qualityProvider.qualityStats(
                page.getRecords().stream().map(SupplierKpi::getSupplierId).toList(), begin, end);
        page.getRecords().forEach(k -> {
            SupplierQualityProvider.QualityStat st = stats.get(k.getSupplierId());
            if (st != null) {
                if (st.iqcBatches() > 0) {
                    k.setIqcBatches(st.iqcBatches());
                    k.setIqcPassBatches(st.iqcPassBatches());
                }
                k.setClaimCount(st.claims());
                k.setClaimAmount(st.claimAmount());
            }
        });
        return PageResult.of(page);
    }

    private void checkCodeUnique(String code, Long id) {
        ScmSupplier exist = supplierMapper.selectOne(Wrappers.<ScmSupplier>lambdaQuery().eq(ScmSupplier::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("scm.error.supplierCodeAlreadyExists");
        }
    }
}
