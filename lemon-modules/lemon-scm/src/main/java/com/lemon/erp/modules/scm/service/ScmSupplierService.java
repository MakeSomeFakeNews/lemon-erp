package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.quality.SupplierQualityProvider;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.dto.SupplierKpi;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.entity.ScmSupplierEval;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScmSupplierService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("code", "供应商编码", "name", "供应商名称", "contact", "联系人", "phone", "联系电话", "email", "邮箱", // i18n-ignore
            "address", "地址", "taxNo", "纳税人识别号", "bankName", "开户行", "bankAccount", "银行账号", "paymentDays", "账期", "remark", "备注"); // i18n-ignore

    private static final int OPTION_LIMIT = 50;

    private final ScmSupplierMapper supplierMapper;
    private final DocNoService docNoService;
    private final ScmSupplierPriceMapper priceMapper;
    private final ScmSupplierEvalMapper evalMapper;
    private final ScmOrderMapper orderMapper;
    private final ReferenceGuard referenceGuard;
    private final ChangeLogger changeLogger;
    private final SupplierQualityProvider qualityProvider;

    /** @param keyword 编码或名称，模糊匹配 */
    public PageResult<ScmSupplier> page(long current, long size, String keyword, SupplierLevel level, Integer status) {
        Page<ScmSupplier> page = supplierMapper.selectPage(Page.of(current, size), Wrappers.<ScmSupplier>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(ScmSupplier::getCode, keyword).or().like(ScmSupplier::getName, keyword))
                .eq(level != null, ScmSupplier::getLevel, level)
                .eq(status != null, ScmSupplier::getStatus, status)
                .orderByAsc(ScmSupplier::getCode));
        return PageResult.of(page);
    }

    public ScmSupplier get(Long id) {
        ScmSupplier supplier = id == null ? null : supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BizException("scm.error.supplierDoesNotExist");
        }
        return supplier;
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

    /** 启用的供应商，按编码或名称搜索 */
    public List<PartnerOption> options(String keyword) {
        return supplierMapper.selectPage(Page.of(1, OPTION_LIMIT, false), Wrappers.<ScmSupplier>lambdaQuery()
                        .eq(ScmSupplier::getStatus, 1)
                        .and(StringUtils.hasText(keyword), w -> w.like(ScmSupplier::getCode, keyword).or().like(ScmSupplier::getName, keyword))
                        .orderByAsc(ScmSupplier::getCode))
                .getRecords().stream()
                .map(s -> new PartnerOption(s.getId(), s.getCode(), s.getName(), s.getContact(), s.getPhone(), s.getAddress(),
                        s.getLevel() == null ? null : s.getLevel().name()))
                .toList();
    }

    @Transactional
    public void create(ScmSupplier supplier) {
        if (!StringUtils.hasText(supplier.getCode())) {
            supplier.setCode(docNoService.next(ScmDocNos.SUPPLIER));
        }
        checkCodeUnique(supplier.getCode(), null);
        supplier.setId(null);
        if (supplier.getStatus() == null) supplier.setStatus(1);
        if (supplier.getPaymentDays() == null) supplier.setPaymentDays(0);
        supplierMapper.insert(supplier);
    }

    public void update(Long id, ScmSupplier supplier) {
        Object before = supplierMapper.selectById(id);
        get(id);
        checkCodeUnique(supplier.getCode(), id);
        supplier.setId(id);
        supplier.setStatus(null);
        if (supplier.getPaymentDays() == null) supplier.setPaymentDays(0);
        supplierMapper.updateById(supplier);
        changeLogger.log("scm_supplier", id, before, supplierMapper.selectById(id), CHANGE_FIELDS);
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
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
