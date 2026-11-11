package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.framework.security.TokenService;
import com.lemon.erp.modules.system.entity.SysAttachment;
import com.lemon.erp.modules.system.entity.SysCustomValue;
import com.lemon.erp.modules.system.entity.SysUser;
import com.lemon.erp.modules.system.mapper.SysAttachmentMapper;
import com.lemon.erp.modules.system.mapper.SysCustomValueMapper;
import com.lemon.erp.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 往来单位合并：
 * <ul>
 *     <li>门户账号（供应商门户 / 经销商门户）改为关联保留的资料，提交后清除这些账号的登录缓存，会话继续有效</li>
 *     <li>附件移到保留的资料下</li>
 *     <li>自定义字段：保留资料没有填写的字段取被合并资料的值，两者都有时保留资料的值优先</li>
 * </ul>
 * 变更历史留在被合并的资料上（可追溯）
 */
@Component
@RequiredArgsConstructor
public class SysPartnerMergeHandler implements PartnerMergeHandler {

    private final SysUserMapper userMapper;
    private final SysAttachmentMapper attachmentMapper;
    private final SysCustomValueMapper customValueMapper;
    private final TokenService tokenService;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        String bizType = bizType(type);
        List<SysCustomValue> values = customValueMapper.selectList(customValues(bizType, sourceIds));
        Set<String> targetKeys = targetKeys(bizType, targetId);
        return List.of(
                new MergeImpact("system.merge.portalAccount", userMapper.selectCount(portalUsers(type, sourceIds)), 0),
                new MergeImpact("system.merge.attachment", attachmentMapper.selectCount(Wrappers.<SysAttachment>lambdaQuery()
                        .eq(SysAttachment::getBizType, bizType).in(SysAttachment::getBizId, sourceIds)), 0),
                new MergeImpact("system.merge.customValue", values.size(),
                        conflicts(values, targetKeys).size()));
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        List<Long> userIds = userMapper.selectList(portalUsers(type, sourceIds).select(SysUser::getId)).stream().map(SysUser::getId).toList();
        if (!userIds.isEmpty()) {
            userMapper.update(Wrappers.<SysUser>lambdaUpdate().set(portalColumn(type), targetId).in(SysUser::getId, userIds));
            userIds.forEach(tokenService::evictLoginUser);
        }
        String bizType = bizType(type);
        attachmentMapper.update(Wrappers.<SysAttachment>lambdaUpdate().set(SysAttachment::getBizId, targetId)
                .eq(SysAttachment::getBizType, bizType).in(SysAttachment::getBizId, sourceIds));
        // 自定义字段：同一字段只保留一个值（唯一约束），保留资料已填写的舍弃被合并资料的值；多份被合并资料填写了同一字段时取 ID 最小的一份
        List<SysCustomValue> values = customValueMapper.selectList(customValues(bizType, sourceIds).orderByAsc(SysCustomValue::getBizId));
        Set<String> taken = targetKeys(bizType, targetId);
        List<Long> drop = values.stream().filter(v -> !taken.add(v.getFieldKey())).map(SysCustomValue::getId).toList();
        if (!drop.isEmpty()) {
            customValueMapper.deleteByIds(drop);
        }
        customValueMapper.update(Wrappers.<SysCustomValue>lambdaUpdate().set(SysCustomValue::getBizId, targetId)
                .eq(SysCustomValue::getBizType, bizType).in(SysCustomValue::getBizId, sourceIds));
    }

    private static List<SysCustomValue> conflicts(List<SysCustomValue> values, Set<String> targetKeys) {
        Set<String> taken = new java.util.HashSet<>(targetKeys);
        return values.stream().filter(v -> !taken.add(v.getFieldKey())).toList();
    }

    private Set<String> targetKeys(String bizType, Long targetId) {
        return customValueMapper.selectList(Wrappers.<SysCustomValue>lambdaQuery().select(SysCustomValue::getFieldKey)
                        .eq(SysCustomValue::getBizType, bizType).eq(SysCustomValue::getBizId, targetId))
                .stream().map(SysCustomValue::getFieldKey).collect(Collectors.toCollection(java.util.HashSet::new));
    }

    private static LambdaQueryWrapper<SysCustomValue> customValues(String bizType, List<Long> sourceIds) {
        return Wrappers.<SysCustomValue>lambdaQuery().select(SysCustomValue::getId, SysCustomValue::getBizId, SysCustomValue::getFieldKey)
                .eq(SysCustomValue::getBizType, bizType).in(SysCustomValue::getBizId, sourceIds);
    }

    /** 附件、自定义字段的业务类型（与 scm 模块登记的记录类型一致） */
    private static String bizType(PartnerType type) {
        return type == PartnerType.CUSTOMER ? "scm_customer" : "scm_supplier";
    }

    private static LambdaQueryWrapper<SysUser> portalUsers(PartnerType type, List<Long> sourceIds) {
        return Wrappers.<SysUser>lambdaQuery().in(portalColumn(type), sourceIds);
    }

    private static SFunction<SysUser, Long> portalColumn(PartnerType type) {
        return type == PartnerType.CUSTOMER ? SysUser::getCustomerId : SysUser::getSupplierId;
    }
}
