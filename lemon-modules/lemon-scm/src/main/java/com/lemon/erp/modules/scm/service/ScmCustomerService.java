package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceGuard;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.modules.scm.dto.PartnerOption;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmOrderMapper;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.SalesChannel;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScmCustomerService {

    /** 记录变更历史的字段 */
    private static final Map<String, String> CHANGE_FIELDS = ChangeLogger.fields("code", "客户编码", "name", "客户名称", "level", "客户等级", "contact", "联系人", "phone", "联系电话",
            "address", "收货地址", "taxNo", "纳税人识别号", "creditLimit", "信用额度", "paymentDays", "账期", "remark", "备注");

    private static final int OPTION_LIMIT = 50;

    private final ScmCustomerMapper customerMapper;
    private final DocNoService docNoService;
    private final ScmOrderMapper orderMapper;
    private final ReferenceGuard referenceGuard;
    private final ChangeLogger changeLogger;
    private final FlowIdentityProvider identityProvider;

    /** @param keyword 编码或名称，模糊匹配 */
    public PageResult<ScmCustomer> page(long current, long size, String keyword, String level, Integer status, SalesChannel channel) {
        Page<ScmCustomer> page = customerMapper.selectPage(Page.of(current, size), Wrappers.<ScmCustomer>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(ScmCustomer::getCode, keyword).or().like(ScmCustomer::getName, keyword))
                .eq(StringUtils.hasText(level), ScmCustomer::getLevel, level)
                .eq(status != null, ScmCustomer::getStatus, status)
                .eq(channel != null, ScmCustomer::getChannel, channel)
                .orderByAsc(ScmCustomer::getCode));
        List<Long> owners = page.getRecords().stream().map(ScmCustomer::getOwnerId).filter(Objects::nonNull).distinct().toList();
        if (!owners.isEmpty()) {
            Map<Long, String> names = identityProvider.getUserNames(owners);
            page.getRecords().forEach(c -> c.setOwnerName(c.getOwnerId() == null ? null : names.get(c.getOwnerId())));
        }
        return PageResult.of(page);
    }

    public ScmCustomer get(Long id) {
        ScmCustomer customer = id == null ? null : customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException("客户不存在");
        }
        return customer;
    }

    /** 启用的客户，按编码或名称搜索 */
    public List<PartnerOption> options(String keyword) {
        return customerMapper.selectPage(Page.of(1, OPTION_LIMIT, false), Wrappers.<ScmCustomer>lambdaQuery()
                        .eq(ScmCustomer::getStatus, 1)
                        .and(StringUtils.hasText(keyword), w -> w.like(ScmCustomer::getCode, keyword).or().like(ScmCustomer::getName, keyword))
                        .orderByAsc(ScmCustomer::getCode))
                .getRecords().stream()
                .map(c -> new PartnerOption(c.getId(), c.getCode(), c.getName(), c.getContact(), c.getPhone(), c.getAddress(), c.getLevel()))
                .toList();
    }

    @Transactional
    public void create(ScmCustomer customer) {
        if (!StringUtils.hasText(customer.getCode())) {
            customer.setCode(docNoService.next(ScmDocNos.CUSTOMER));
        }
        checkCodeUnique(customer.getCode(), null);
        customer.setId(null);
        if (customer.getStatus() == null) customer.setStatus(1);
        if (customer.getPaymentDays() == null) customer.setPaymentDays(0);
        if (customer.getChannel() == null) customer.setChannel(SalesChannel.DIRECT);
        customerMapper.insert(customer);
    }

    public void update(Long id, ScmCustomer customer) {
        Object before = customerMapper.selectById(id);
        ScmCustomer old = get(id);
        checkCodeUnique(customer.getCode(), id);
        customer.setId(id);
        customer.setStatus(null);
        if (customer.getPaymentDays() == null) customer.setPaymentDays(0);
        if (customer.getChannel() == null) customer.setChannel(SalesChannel.DIRECT);
        customerMapper.updateById(customer);
        // 清空信用额度（不限制）时 updateById 会跳过 null 字段，单独更新
        if (customer.getCreditLimit() == null && old.getCreditLimit() != null) {
            customerMapper.update(Wrappers.<ScmCustomer>lambdaUpdate().set(ScmCustomer::getCreditLimit, null).eq(ScmCustomer::getId, id));
        }
        changeLogger.log("scm_customer", id, before, customerMapper.selectById(id), CHANGE_FIELDS);
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        ScmCustomer entity = new ScmCustomer();
        entity.setId(id);
        entity.setStatus(status);
        customerMapper.updateById(entity);
    }

    /** 有订单的客户不能删除，可以停用 */
    public void delete(List<Long> ids) {
        for (ScmCustomer customer : customerMapper.selectByIds(ids)) {
            if (orderMapper.exists(Wrappers.<ScmOrder>lambdaQuery()
                    .eq(ScmOrder::getKind, OrderKind.SALES.name()).eq(ScmOrder::getPartnerId, customer.getId()))) {
                throw new BizException("客户「" + customer.getName() + "」已有销售订单，不能删除，可以停用");
            }
            referenceGuard.checkDeletable(RefType.CUSTOMER, customer.getId(), "客户「" + customer.getName() + "」");
        }
        customerMapper.deleteByIds(ids);
    }

    private void checkCodeUnique(String code, Long id) {
        ScmCustomer exist = customerMapper.selectOne(Wrappers.<ScmCustomer>lambdaQuery().eq(ScmCustomer::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("客户编码已存在");
        }
    }
}
