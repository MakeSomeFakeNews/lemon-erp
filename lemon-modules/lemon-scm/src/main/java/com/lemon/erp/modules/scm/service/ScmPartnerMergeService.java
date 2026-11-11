package com.lemon.erp.modules.scm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.scm.dto.PartnerMergeRequests;
import com.lemon.erp.modules.scm.dto.PartnerMergeRequests.Brief;
import com.lemon.erp.modules.scm.dto.PartnerMergeRequests.Preview;
import com.lemon.erp.modules.scm.entity.ScmCustomer;
import com.lemon.erp.modules.scm.entity.ScmSupplier;
import com.lemon.erp.modules.scm.mapper.ScmCustomerMapper;
import com.lemon.erp.modules.scm.mapper.ScmSupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 往来单位合并（issue #95）：同一客户（或供应商）建了多份资料时，把被合并资料的全部业务引用改为指向保留的资料，
 * 被合并的资料停用并标记「已合并到」（保留资料本身，可追溯，不能再启用；之后没有引用时可以删除）。
 * <p>
 * 各模块实现 {@link PartnerMergeHandler} 迁移自己的表（订单、出入库、往来、凭证辅助核算、CRM、项目、门户账号、BI 事实表等），
 * 在一个事务中完成，任一模块报告冲突或出错时整体回滚。规则：
 * <ul>
 *     <li>保留资料的属性（名称、信用额度、账期、价格等级、负责销售、等级等）不变，被合并资料的属性不带过来；
 *     单据上的往来单位名称快照（出入库单、库存流水）改为保留资料的名称</li>
 *     <li>币种：往来单位没有币种，币种记在每张单据上，合并后各单据仍按自己的币种核销</li>
 *     <li>价格冲突（同一物料）：客户执行价、供货价格以保留资料为准，价格记忆合为一行（见 {@link ScmPartnerMergeHandler}）</li>
 *     <li>关联（供应商的关联客户，一对一）：合并后最多只能有一个关联，涉及的资料各自关联了不同的单位时拒绝合并（先解除其中的关联）</li>
 *     <li>不能自动处理的冲突（同一询价单、同一招标、同一期间的渠道库存）拒绝合并</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ScmPartnerMergeService {

    private static final Map<String, String> TRACE_FIELDS = ChangeLogger.fields("mergedTo", "合并到", "mergedFrom", "合并的资料"); // i18n-ignore

    private final ScmCustomerMapper customerMapper;
    private final ScmSupplierMapper supplierMapper;
    private final ObjectProvider<PartnerMergeHandler> handlers;
    private final ChangeLogger changeLogger;

    /** 合并的变更记录：被合并资料记「合并到」，保留资料记「合并的资料」 */
    public record Trace(String mergedTo, String mergedFrom) {
    }

    /** 合并前预览：校验资料与冲突，返回影响范围 */
    public Preview preview(PartnerType type, PartnerMergeRequests.Merge req) {
        Context ctx = load(type, req);
        return check(type, ctx);
    }

    private Preview check(PartnerType type, Context ctx) {
        List<Long> sourceIds = ctx.sourceIds();
        handlers.orderedStream().forEach(h -> h.check(type, sourceIds, ctx.target().id()));
        return new Preview(ctx.target(), ctx.sources(), impacts(type, sourceIds, ctx.target().id()), ctx.linkedName());
    }

    /** 执行合并，返回合并前统计的影响范围 */
    @Transactional
    public Preview merge(PartnerType type, PartnerMergeRequests.Merge req) {
        Context ctx = load(type, req);
        Preview preview = check(type, ctx);
        Long targetId = ctx.target().id();
        List<Long> sourceIds = ctx.sourceIds();
        relink(type, ctx);
        handlers.orderedStream().forEach(h -> h.merge(type, sourceIds, targetId, ctx.target().name()));
        String targetText = ctx.target().code() + " " + ctx.target().name();
        if (type == PartnerType.CUSTOMER) {
            customerMapper.update(Wrappers.<ScmCustomer>lambdaUpdate().set(ScmCustomer::getStatus, EnableStatus.DISABLED)
                    .set(ScmCustomer::getMergedTo, targetId).set(ScmCustomer::getUpdateTime, LocalDateTime.now())
                    .in(ScmCustomer::getId, sourceIds));
        } else {
            supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate().set(ScmSupplier::getStatus, EnableStatus.DISABLED)
                    .set(ScmSupplier::getMergedTo, targetId).set(ScmSupplier::getUpdateTime, LocalDateTime.now())
                    .in(ScmSupplier::getId, sourceIds));
        }
        String bizType = type == PartnerType.CUSTOMER ? "scm_customer" : "scm_supplier";
        ctx.sources().forEach(s -> changeLogger.log(bizType, s.id(), new Trace(null, null), new Trace(targetText, null), TRACE_FIELDS));
        String sourcesText = ctx.sources().stream().map(s -> s.code() + " " + s.name()).collect(Collectors.joining("、"));
        changeLogger.log(bizType, targetId, new Trace(null, null), new Trace(null, sourcesText), TRACE_FIELDS);
        return preview;
    }

    /** 各模块的影响范围按消息键汇总（同一类数据可能来自几张表），只保留有引用的，名称按当前语言翻译 */
    private List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        Map<String, MergeImpact> sum = new LinkedHashMap<>();
        handlers.orderedStream().flatMap(h -> h.impacts(type, sourceIds, targetId).stream()).forEach(i -> sum.merge(i.getLabelKey(),
                new MergeImpact(i.getLabelKey(), i.getTotal(), i.getConflicts()),
                (a, b) -> new MergeImpact(a.getLabelKey(), a.getTotal() + b.getTotal(), a.getConflicts() + b.getConflicts())));
        List<MergeImpact> list = sum.values().stream().filter(i -> i.getTotal() > 0).toList();
        list.forEach(i -> i.setLabel(Messages.get(i.getLabelKey())));
        return list;
    }

    /**
     * 关联（一对一）：合并客户时，关联被合并客户的供应商改为关联保留的客户；合并供应商时，保留的供应商没有关联客户而被合并的有，
     * 关联转到保留的供应商（先清除被合并供应商的关联，避免唯一约束冲突）
     */
    private void relink(PartnerType type, Context ctx) {
        if (ctx.linkedId() == null) {
            return;
        }
        Long targetId = ctx.target().id();
        if (type == PartnerType.CUSTOMER) {
            supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate().set(ScmSupplier::getCustomerId, targetId)
                    .eq(ScmSupplier::getId, ctx.linkedId()));
        } else {
            supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate().set(ScmSupplier::getCustomerId, null)
                    .in(ScmSupplier::getId, ctx.sourceIds()));
            supplierMapper.update(Wrappers.<ScmSupplier>lambdaUpdate().set(ScmSupplier::getCustomerId, ctx.linkedId())
                    .eq(ScmSupplier::getId, targetId));
        }
    }

    /**
     * @param linkedId   合并后保留资料的关联单位（客户合并时为供应商 ID，供应商合并时为客户 ID），没有为 null
     * @param linkedName 关联单位的编码 名称
     */
    private record Context(Brief target, List<Brief> sources, Long linkedId, String linkedName) {

        List<Long> sourceIds() {
            return sources.stream().map(Brief::id).toList();
        }
    }

    private Context load(PartnerType type, PartnerMergeRequests.Merge req) {
        List<Long> sourceIds = req.sourceIds().stream().filter(Objects::nonNull).distinct().toList();
        if (sourceIds.isEmpty()) {
            throw new BizException("scm.valid.mergeSourcesRequired");
        }
        if (sourceIds.size() > PartnerMergeRequests.MAX_SOURCES) {
            throw new BizException("scm.valid.mergeSourcesTooMany");
        }
        if (sourceIds.contains(req.targetId())) {
            throw new BizException("scm.error.mergeSourceIsTarget");
        }
        return type == PartnerType.CUSTOMER ? loadCustomers(req.targetId(), sourceIds) : loadSuppliers(req.targetId(), sourceIds);
    }

    private Context loadCustomers(Long targetId, List<Long> sourceIds) {
        ScmCustomer target = customerMapper.selectById(targetId);
        List<ScmCustomer> sources = customerMapper.selectByIds(sourceIds);
        if (target == null || sources.size() != sourceIds.size()) {
            throw new BizException("scm.error.customerDoesNotExist");
        }
        checkNotMerged(target.getMergedTo(), target.getName(), sources.stream().filter(s -> s.getMergedTo() != null)
                .map(ScmCustomer::getName).findFirst().orElse(null));
        // 关联这些客户的供应商：合并后只能有一个
        List<ScmSupplier> linked = supplierMapper.selectList(Wrappers.<ScmSupplier>lambdaQuery()
                .select(ScmSupplier::getId, ScmSupplier::getCode, ScmSupplier::getName, ScmSupplier::getCustomerId)
                .in(ScmSupplier::getCustomerId, Stream.concat(Stream.of(targetId), sourceIds.stream()).toList()));
        if (linked.size() > 1) {
            throw new BizException("scm.error.mergeLinkConflict",
                    linked.stream().map(s -> s.getCode() + " " + s.getName()).collect(Collectors.joining("、")));
        }
        Map<Long, String> linkedNames = linked.stream().collect(Collectors.toMap(ScmSupplier::getCustomerId, s -> s.getCode() + " " + s.getName()));
        ScmSupplier link = linked.isEmpty() ? null : linked.get(0);
        return new Context(brief(target, linkedNames), sources.stream().map(c -> brief(c, linkedNames)).toList(),
                link == null ? null : link.getId(), link == null ? null : link.getCode() + " " + link.getName());
    }

    private Context loadSuppliers(Long targetId, List<Long> sourceIds) {
        ScmSupplier target = supplierMapper.selectById(targetId);
        List<ScmSupplier> sources = supplierMapper.selectByIds(sourceIds);
        if (target == null || sources.size() != sourceIds.size()) {
            throw new BizException("scm.error.supplierDoesNotExist");
        }
        checkNotMerged(target.getMergedTo(), target.getName(), sources.stream().filter(s -> s.getMergedTo() != null)
                .map(ScmSupplier::getName).findFirst().orElse(null));
        // 这些供应商关联的客户：合并后只能有一个
        List<Long> customerIds = Stream.concat(Stream.of(target), sources.stream()).map(ScmSupplier::getCustomerId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> customerNames = customerIds.isEmpty() ? Collections.<Long, String>emptyMap() : customerMapper.selectByIds(customerIds).stream()
                .collect(Collectors.toMap(ScmCustomer::getId, c -> c.getCode() + " " + c.getName()));
        if (customerIds.size() > 1) {
            throw new BizException("scm.error.mergeLinkConflict", customerIds.stream().map(customerNames::get)
                    .filter(Objects::nonNull).collect(Collectors.joining("、")));
        }
        Long link = customerIds.isEmpty() ? null : customerIds.get(0);
        return new Context(brief(target, customerNames), sources.stream().map(s -> brief(s, customerNames)).toList(),
                link, link == null ? null : customerNames.get(link));
    }

    private static void checkNotMerged(Long targetMergedTo, String targetName, String mergedSource) {
        if (targetMergedTo != null) {
            throw new BizException("scm.error.mergeTargetMerged", targetName);
        }
        if (mergedSource != null) {
            throw new BizException("scm.error.mergeSourceMerged", mergedSource);
        }
    }

    private static Brief brief(ScmCustomer c, Map<Long, String> linkedSuppliers) {
        return new Brief(c.getId(), c.getCode(), c.getName(), c.getStatus(), linkedSuppliers.get(c.getId()));
    }

    private static Brief brief(ScmSupplier s, Map<Long, String> customerNames) {
        return new Brief(s.getId(), s.getCode(), s.getName(), s.getStatus(), s.getCustomerId() == null ? null : customerNames.get(s.getCustomerId()));
    }
}
