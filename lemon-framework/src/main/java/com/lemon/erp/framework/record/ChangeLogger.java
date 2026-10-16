package com.lemon.erp.framework.record;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字段变更历史：业务模块在修改关键单据、主数据后调用，比较修改前后的字段并记录有变化的字段（谁、何时、旧值、新值）。
 * 由系统模块实现，与业务修改在同一事务中写入
 */
public interface ChangeLogger {

    /**
     * @param bizType 业务类型（{@link RecordType#code()}），按它校验查看变更历史的权限
     * @param before  修改前的记录（getter 或 record 访问器读取字段）
     * @param after   修改后的记录，一般为修改后重新查询的实体
     * @param fields  需要记录的字段 → 显示名称，见 {@link #fields(String...)}
     */
    void log(String bizType, Long bizId, Object before, Object after, Map<String, String> fields);

    /** 字段定义：fields("name", "名称", "spec", "规格型号") */
    static Map<String, String> fields(String... nameLabelPairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < nameLabelPairs.length; i += 2) {
            map.put(nameLabelPairs[i], nameLabelPairs[i + 1]);
        }
        return map;
    }
}
