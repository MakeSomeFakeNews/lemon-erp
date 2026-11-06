package com.lemon.erp.framework.ai;

import java.util.List;

/**
 * 工具的返回结果。工具可以直接返回任意可序列化为 JSON 的对象（record、List、Map），也可以返回本类型以说明是否截断。
 * <p>
 * <b>行数上限</b>：结果为列表时最多 {@link AiTool#MAX_ROWS} 行，超出部分由 AI 模块截断并告诉模型「结果已截断、共多少行」，
 * 模型会提示用户缩小范围。工具自己分页查询时建议多查 1 行判断是否还有更多，用 {@link #rows(List, Long)} 返回总数。
 *
 * @param data      结果数据（列表、对象），会序列化为 JSON 回传给模型，并以表格显示给用户
 * @param total     符合条件的总行数（不知道时为 null）
 * @param truncated 是否只返回了部分数据
 */
public record AiToolResult(Object data, Long total, boolean truncated) {

    /** 列表结果：超过上限时截断 */
    public static AiToolResult rows(List<?> rows) {
        return rows(rows, null);
    }

    /**
     * 列表结果
     *
     * @param total 总行数（为 null 时按 rows 的行数）；大于返回的行数时视为截断
     */
    public static AiToolResult rows(List<?> rows, Long total) {
        long count = total == null ? rows.size() : total;
        if (rows.size() > AiTool.MAX_ROWS) {
            return new AiToolResult(List.copyOf(rows.subList(0, AiTool.MAX_ROWS)), count, true);
        }
        return new AiToolResult(rows, count, count > rows.size());
    }

    /** 单个对象（汇总指标等），不截断 */
    public static AiToolResult of(Object data) {
        return new AiToolResult(data, null, false);
    }
}
