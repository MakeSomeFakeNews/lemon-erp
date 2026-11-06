package com.lemon.erp.framework.ai;

import com.lemon.erp.common.result.PageResult;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据整理的主数据来源（物料、客户、供应商等）：AI 分页读取候选记录做查重、名称 / 规格规范化、分类建议，
 * 用户逐条确认后由 {@link #apply(AiCleanseChange)} 应用。业务模块实现为 Spring Bean，AI 模块收集。
 *
 * <p><b>实现约定</b>
 * <ul>
 *   <li>{@link #page} 调用本模块 Service 的查询（数据权限生效），只返回 {@link AiCleanseRecord} 中的字段</li>
 *   <li>{@link #apply} 调用本模块 Service 的修改方法（与手工修改相同的校验、变更历史），不要直接写表；
 *       多个字段一起修改时在一个事务中完成</li>
 * </ul>
 */
public interface AiCleanseSource {

    /** 标识的格式：模块.名称，小写字母、数字、连字符，如 {@code wms.material} */
    Pattern KEY_PATTERN = AiExtractTarget.KEY_PATTERN;

    /** 全局唯一的标识，如 {@code wms.material} */
    String key();

    /** 名称的消息键（如「物料」） */
    String labelKey();

    /** 查看候选记录、生成建议所需的权限（如 {@code wms:material:list}）；为 null 表示登录即可 */
    String viewPermission();

    /** 应用建议所需的权限（如 {@code wms:material:edit}） */
    String applyPermission();

    /** 可以修改的字段 */
    Set<AiCleanseField> editableFields();

    /**
     * 分页读取候选记录（按 ID 排序，保证分页稳定）。只返回启用的记录（停用的不再参与查重和规范化）
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数（AI 模块不超过 500）
     * @param keyword 编码或名称关键字，为 null 时不过滤
     */
    PageResult<AiCleanseRecord> page(long current, long size, String keyword);

    /**
     * 按 ID 读取一条记录的当前值：应用建议前用它检查记录在生成建议之后是否被修改过（被修改过的建议不再应用）。
     * 返回 null 表示不支持（不做检查）
     */
    default AiCleanseRecord get(Long id) {
        return null;
    }

    /**
     * 分类的可选值（值 → 名称，如业务字典），提供给模型做分类建议，并用于校验模型给出的分类；
     * 没有分类或分类不是固定取值时返回空 Map
     */
    default Map<String, String> categoryOptions() {
        return Map.of();
    }

    /**
     * 应用一条用户确认过的建议
     *
     * @throws com.lemon.erp.common.exception.BizException 记录不存在、没有数据权限、校验不通过（如名称重复）时
     */
    void apply(AiCleanseChange change);

    /** 是否支持停用（疑似重复的记录只建议停用，不自动合并：合并涉及大量业务引用） */
    default boolean supportsDisable() {
        return false;
    }

    /**
     * 停用一条记录（调用本模块 Service 的停用方法），权限同 {@link #applyPermission()}
     *
     * @throws com.lemon.erp.common.exception.BizException 记录不存在等
     */
    default void disable(Long id) {
        throw new UnsupportedOperationException(key() + " does not support disable");
    }
}
