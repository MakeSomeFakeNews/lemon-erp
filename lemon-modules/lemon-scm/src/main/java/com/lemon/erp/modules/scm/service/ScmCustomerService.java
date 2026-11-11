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
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.SalesChannel;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScmCustomerService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("code", "客户编码", "name", "客户名称", "level", "客户等级", "priceLevel", "价格等级", "contact", "联系人", "phone", "联系电话", // i18n-ignore
            "address", "收货地址", "taxNo", "纳税人识别号", "creditLimit", "信用额度", "paymentDays", "账期", "region", "区域", "remark", "备注"); // i18n-ignore

    /** 下拉搜索每页最多返回的条数 */
    public static final int OPTION_LIMIT = 50;

    /** 区域（业务字典） */
    public static final String REGION_DICT = "scm_region";

    private final ScmCustomerMapper customerMapper;
    private final ScmSupplierMapper supplierMapper;
    private final DocNoService docNoService;
    private final ScmOrderMapper orderMapper;
    private final ReferenceGuard referenceGuard;
    private final ChangeLogger changeLogger;
    private final FlowIdentityProvider identityProvider;
    private final DictLookup dictLookup;
    private final SearchKeyTable<ScmCustomer> searchKeys;

    /**
     * @param keyword 编码、名称、联系人、电话、拼音首字母或去掉分隔符的缩写，规则见 {@link SearchKeys}
     * @param region  区域（字典 scm_region 的值）
     */
    public PageResult<ScmCustomer> page(long current, long size, String keyword, String level, EnableStatus status, SalesChannel channel,
                                        String region) {
        Page<ScmCustomer> page = customerMapper.selectPage(Page.of(current, size), Wrappers.<ScmCustomer>lambdaQuery()
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .eq(StringUtils.hasText(level), ScmCustomer::getLevel, level)
                .eq(status != null, ScmCustomer::getStatus, status)
                .eq(channel != null, ScmCustomer::getChannel, channel)
                .eq(StringUtils.hasText(region), ScmCustomer::getRegion, region)
                .orderByAsc(ScmCustomer::getCode));
        List<Long> owners = page.getRecords().stream().map(ScmCustomer::getOwnerId).filter(Objects::nonNull).distinct().toList();
        if (!owners.isEmpty()) {
            Map<Long, String> names = identityProvider.getUserNames(owners);
            page.getRecords().forEach(c -> c.setOwnerName(c.getOwnerId() == null ? null : names.get(c.getOwnerId())));
        }
        fillNames(page.getRecords());
        return PageResult.of(page);
    }

    /** 已合并到的客户、关联的供应商的显示名（编码 名称） */
    private void fillNames(List<ScmCustomer> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> mergedIds = rows.stream().map(ScmCustomer::getMergedTo).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> merged = mergedIds.isEmpty() ? Collections.<Long, String>emptyMap() : customerMapper.selectByIds(mergedIds).stream()
                .collect(Collectors.toMap(ScmCustomer::getId, x -> x.getCode() + " " + x.getName()));
        Map<Long, String> suppliers = supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery()
                        .select(ScmSupplier::getCustomerId, ScmSupplier::getCode, ScmSupplier::getName)
                        .in(ScmSupplier::getCustomerId, rows.stream().map(ScmCustomer::getId).toList()))
                .stream().collect(Collectors.toMap(ScmSupplier::getCustomerId, x -> x.getCode() + " " + x.getName(), (a, b) -> a));
        rows.forEach(r -> {
            r.setMergedToName(merged.get(r.getMergedTo()));
            r.setLinkedSupplierName(suppliers.get(r.getId()));
        });
    }

    /** 详情（编辑表单）：带已合并到的客户、关联的供应商的名称 */
    public ScmCustomer detail(Long id) {
        ScmCustomer customer = get(id);
        fillNames(List.of(customer));
        return customer;
    }

    public ScmCustomer get(Long id) {
        ScmCustomer customer = id == null ? null : customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException("scm.error.customerDoesNotExist");
        }
        return customer;
    }

    /** 校验客户的渠道为经销商（开通经销商门户账号前） */
    public void requireDealer(Long id) {
        if (get(id).getChannel() != SalesChannel.DEALER) {
            throw new BizException("scm.error.onlyCustomersDealerChannelHaveDealer");
        }
    }

    /**
     * 按税号精确匹配客户（不限状态），供财务模块的发票导入等识别往来单位
     *
     * @return 第一个匹配的客户 ID，没有时返回 null
     */
    public Long findIdByTaxNo(String taxNo) {
        return firstId(customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getTaxNo, taxNo)));
    }

    /**
     * 按名称精确匹配客户（不限状态），供财务模块的发票导入等识别往来单位
     *
     * @return 第一个匹配的客户 ID，没有时返回 null
     */
    public Long findIdByName(String name) {
        return firstId(customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getName, name)));
    }

    /**
     * 按编码或名称精确匹配客户（不限状态），供财务模块的期初导入等按文字识别往来单位。编码完全一致的优先
     *
     * @return 客户 ID，没有时返回 null
     */
    public Long findIdByCodeOrName(String key) {
        List<ScmCustomer> list = customerMapper.selectList(Wrappers.<ScmCustomer>lambdaQuery()
                .eq(ScmCustomer::getCode, key).or().eq(ScmCustomer::getName, key));
        if (list.isEmpty()) {
            return null;
        }
        return list.stream().filter(x -> key.equals(x.getCode())).findFirst().orElse(list.get(0)).getId();
    }

    /** 按编码精确查找客户（不限状态），没有时返回 null */
    public ScmCustomer findByCode(String code) {
        return code == null ? null : customerMapper.selectOne(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getCode, code));
    }

    private static Long firstId(List<ScmCustomer> list) {
        return list.isEmpty() ? null : list.get(0).getId();
    }

    /** 启用的客户，按关键字搜索，最多 {@value #OPTION_LIMIT} 条 */
    public List<PartnerOption> options(String keyword) {
        return options(keyword, 1, OPTION_LIMIT);
    }

    /**
     * 启用的客户下拉：按关键字（规则见 {@link SearchKeys}）分页搜索，按编码排序，不查总数；
     * 第一页中编码与关键字完全一致的客户排在最前
     *
     * @param size 每页条数，最多 {@value #OPTION_LIMIT}
     */
    public List<PartnerOption> options(String keyword, long current, long size) {
        List<ScmCustomer> rows = customerMapper.selectPage(Page.of(Math.max(current, 1), Math.min(Math.max(size, 1), OPTION_LIMIT), false),
                Wrappers.<ScmCustomer>lambdaQuery()
                        .eq(ScmCustomer::getStatus, EnableStatus.ENABLED)
                        .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                        .orderByAsc(ScmCustomer::getCode)).getRecords();
        if (StringUtils.hasText(keyword) && current <= 1) {
            ScmCustomer exact = customerMapper.selectOne(Wrappers.<ScmCustomer>lambdaQuery()
                    .eq(ScmCustomer::getStatus, EnableStatus.ENABLED).eq(ScmCustomer::getCode, keyword.trim()));
            rows = SearchKeys.exactFirst(rows, exact, ScmCustomer::getId);
        }
        return rows.stream().map(ScmCustomerService::toOption).toList();
    }

    /** 按 ID 取客户（含停用的，最多 {@value #OPTION_LIMIT} 个），下拉回显已选的值 */
    public List<PartnerOption> optionsByIds(List<Long> ids) {
        return ids.isEmpty() ? List.of() : customerMapper.selectByIds(ids.stream().distinct().limit(OPTION_LIMIT).toList())
                .stream().map(ScmCustomerService::toOption).toList();
    }

    private static PartnerOption toOption(ScmCustomer c) {
        return new PartnerOption(c.getId(), c.getCode(), c.getName(), c.getContact(), c.getPhone(), c.getAddress(), c.getLevel());
    }

    /**
     * 数据整理的候选记录：启用的客户，按 ID 升序分页，只查分析需要的列（不含电话、银行账号等）
     *
     * @param keyword 编码或名称，模糊匹配
     */
    public PageResult<ScmCustomer> pageForCleanse(long current, long size, String keyword) {
        Page<ScmCustomer> page = customerMapper.selectPage(Page.of(current, size), Wrappers.<ScmCustomer>lambdaQuery()
                .select(ScmCustomer::getId, ScmCustomer::getCode, ScmCustomer::getName, ScmCustomer::getContact, ScmCustomer::getAddress, ScmCustomer::getTaxNo)
                .eq(ScmCustomer::getStatus, EnableStatus.ENABLED)
                .and(StringUtils.hasText(keyword), keywordMatch(keyword))
                .orderByAsc(ScmCustomer::getId));
        return PageResult.of(page);
    }

    /** 关键字匹配：编码、名称、联系人、电话包含关键字，或检索列包含规范化的关键字 */
    private static Consumer<LambdaQueryWrapper<ScmCustomer>> keywordMatch(String keyword) {
        return SearchKeys.matches(keyword, ScmCustomer::getSearchKey,
                ScmCustomer::getCode, ScmCustomer::getName, ScmCustomer::getContact, ScmCustomer::getPhone);
    }

    /** 是否有其他启用的客户使用该名称 */
    public boolean existsEnabledName(String name, Long excludeId) {
        return customerMapper.exists(Wrappers.<ScmCustomer>lambdaQuery()
                .eq(ScmCustomer::getName, name)
                .eq(ScmCustomer::getStatus, EnableStatus.ENABLED)
                .ne(excludeId != null, ScmCustomer::getId, excludeId));
    }

    @Transactional
    public void create(ScmCustomer customer) {
        if (!StringUtils.hasText(customer.getCode())) {
            customer.setCode(docNoService.next(ScmDocNos.CUSTOMER));
        }
        checkCodeUnique(customer.getCode(), null);
        customer.setRegion(normalizeRegion(customer.getRegion(), null));
        customer.setId(null);
        if (customer.getStatus() == null) customer.setStatus(EnableStatus.ENABLED);
        if (customer.getPaymentDays() == null) customer.setPaymentDays(0);
        if (customer.getChannel() == null) customer.setChannel(SalesChannel.DIRECT);
        if (customer.getPriceLevel() == null) customer.setPriceLevel(0);
        customerMapper.insert(customer);
    }

    @Transactional
    public void update(Long id, ScmCustomer customer) {
        Object before = customerMapper.selectById(id);
        ScmCustomer old = get(id);
        checkCodeUnique(customer.getCode(), id);
        customer.setRegion(normalizeRegion(customer.getRegion(), old.getRegion()));
        customer.setId(id);
        customer.setStatus(null);
        if (customer.getPaymentDays() == null) customer.setPaymentDays(0);
        if (customer.getChannel() == null) customer.setChannel(SalesChannel.DIRECT);
        customerMapper.updateById(customer);
        // 清空信用额度（不限制）时 updateById 会跳过 null 字段，单独更新
        if (customer.getCreditLimit() == null && old.getCreditLimit() != null) {
            customerMapper.update(Wrappers.<ScmCustomer>lambdaUpdate().set(ScmCustomer::getCreditLimit, null).eq(ScmCustomer::getId, id));
        }
        ScmCustomer after = customerMapper.selectById(id);
        searchKeys.refresh(after);
        changeLogger.log("scm_customer", id, before, after, CHANGE_FIELDS);
    }

    public void changeStatus(Long id, EnableStatus status) {
        ScmCustomer old = detail(id);
        if (old.getMergedTo() != null && status == EnableStatus.ENABLED) {
            throw new BizException("scm.error.mergedCannotEnable", old.getName(), old.getMergedToName());
        }
        ScmCustomer entity = new ScmCustomer();
        entity.setId(id);
        entity.setStatus(status);
        customerMapper.updateById(entity);
    }

    /** 有订单的客户不能删除，可以停用；删除时解除供应商对它的关联 */
    @Transactional
    public void delete(List<Long> ids) {
        for (ScmCustomer customer : customerMapper.selectByIds(ids)) {
            if (orderMapper.exists(Wrappers.<ScmOrder>lambdaQuery()
                    .eq(ScmOrder::getKind, OrderKind.SALES).eq(ScmOrder::getPartnerId, customer.getId()))) {
                throw new BizException("scm.error.customerHasSalesOrdersCannotDeleted", customer.getName());
            }
            referenceGuard.checkDeletable(RefType.CUSTOMER, customer.getId(), Messages.get("scm.text.customer", customer.getName()));
        }
        supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate().set(ScmSupplier::getCustomerId, null).in(ScmSupplier::getCustomerId, ids));
        customerMapper.deleteByIds(ids);
    }

    /**
     * 区域须为业务字典 scm_region 中启用的值；未修改的（字典值后来停用）不再校验。CRM 客户、线索的地区共用此字典
     *
     * @param old 修改前的值，新增时为 null
     * @return 去掉首尾空格的值，空白为 null
     */
    public String normalizeRegion(String region, String old) {
        String r = StringUtils.hasText(region) ? region.trim() : null;
        if (r != null && !r.equals(old)) {
            dictLookup.check(REGION_DICT, r, "scm.label.region");
        }
        return r;
    }

    /** 名称或键值包含关键字的区域（字典 scm_region 的值），用于按区域名称搜索 */
    public List<String> regionsMatching(String keyword) {
        return dictLookup.options(REGION_DICT).entrySet().stream()
                .filter(e -> e.getKey().contains(keyword) || e.getValue() != null && e.getValue().contains(keyword))
                .map(Map.Entry::getKey).toList();
    }

    private void checkCodeUnique(String code, Long id) {
        ScmCustomer exist = customerMapper.selectOne(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("scm.error.customerCodeAlreadyExists");
        }
    }
}
