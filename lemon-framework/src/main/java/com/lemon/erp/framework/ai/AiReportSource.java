package com.lemon.erp.framework.ai;

import java.util.regex.Pattern;

/**
 * 智能报告（周报 / 月报）的数据来源：按期间返回结构化的指标快照，AI 模块把有权限的各来源快照交给模型撰写报告。
 * 业务模块实现为 Spring Bean，AI 模块收集。
 *
 * <p><b>实现约定</b>
 * <ul>
 *   <li>{@link #snapshot} 只查询，返回 record（可嵌套 List），字段名用有含义的英文驼峰，金额单位为元；
 *       指标是汇总值和少量排行（如前 10 名客户），不返回明细清单</li>
 *   <li>需要环比时自己用 {@link AiReportPeriod#previous()} 查询上期并放在快照中，模型不做计算</li>
 *   <li>数据权限由本模块 Service 的查询自动生效；汇总全公司数据的指标要求相应的权限</li>
 * </ul>
 */
public interface AiReportSource {

    /** 标识的格式：模块.名称，如 {@code scm.sales} */
    Pattern KEY_PATTERN = AiExtractTarget.KEY_PATTERN;

    /** 全局唯一的标识，如 {@code scm.sales} */
    String key();

    /** 名称的消息键（如「销售」），报告中作为章节标题 */
    String labelKey();

    /** 所需的权限；没有权限的用户生成报告时不包含该来源。为 null 表示登录即可 */
    String permission();

    /**
     * 是否只对有「全部数据」权限的用户开放：汇总全公司数据、底层查询不按数据权限过滤的来源（如财务、库存、人事）返回 true，
     * 与驾驶舱的规则一致；底层查询按数据权限自动过滤的来源返回 false（报告中注明数据范围）
     */
    default boolean requiresAllData() {
        return false;
    }

    /** 报告中板块的顺序（小的在前），默认 100 */
    default int order() {
        return 100;
    }

    /**
     * 期间内的指标快照
     *
     * @return 可序列化为 JSON 的 record
     */
    Object snapshot(AiReportPeriod period);
}
