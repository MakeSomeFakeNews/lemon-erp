package com.lemon.erp.framework.ref;

import java.util.List;

/**
 * 往来单位合并：同一客户（或供应商）建了多份资料时，把被合并资料的全部业务引用改为指向保留的资料。
 * 供应链模块的合并服务在一个事务中依次调用各模块的实现；上游模块看不到下游模块的表，每个模块只迁移自己的表
 * （与 {@link ReferenceChecker} 相同，每个模块一个 {@code XxxPartnerMergeHandler} + {@code XxxMergeMapper.xml}）。
 * 新增业务表引用客户 / 供应商时，在所属模块的实现中同时登记影响范围和迁移语句。
 * 同一列存放客户或供应商的表（如 partner_id）必须按种类区分（订单种类、单据方向等），客户和供应商的 ID 会重复
 */
public interface PartnerMergeHandler {

    /** 合并前预览：本模块引用被合并资料的各类数据的行数，没有引用的可以不返回 */
    List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId);

    /** 合并前检查不能自动处理的冲突（如两份资料参与了同一张询价单），有冲突时抛出 BizException；默认没有 */
    default void check(PartnerType type, List<Long> sourceIds, Long targetId) {
    }

    /**
     * 把本模块引用被合并资料的数据改为指向保留的资料（与合并在同一事务中）
     *
     * @param targetName 保留资料的名称，单据上有名称快照的一并更新
     */
    void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName);
}
