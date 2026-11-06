package com.lemon.erp.modules.fin.ai;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

/**
 * 财务模块 AI 问数工具的公共参数：日期范围（默认本月 1 日至今天）、会计期间（默认本月）、关键字、返回行数
 */
final class FinAiSupport {

    static final String START_DATE = "startDate";
    static final String END_DATE = "endDate";
    static final String PERIOD = "period";
    static final String KEYWORD = "keyword";
    static final String TOP = "top";
    static final int DEFAULT_TOP = 20;

    private FinAiSupport() {
    }

    static AiSchema dateRange(AiSchema schema) {
        return schema.prop(START_DATE, AiSchema.date("fin.ai.param.startDate"), false)
                .prop(END_DATE, AiSchema.date("fin.ai.param.endDate"), false);
    }

    static AiSchema period(AiSchema schema) {
        return schema.prop(PERIOD, AiSchema.integer("fin.ai.param.period").min(200001).max(209912), false);
    }

    static AiSchema keyword(AiSchema schema, String descriptionKey) {
        return schema.prop(KEYWORD, AiSchema.string(descriptionKey).maxLength(64), false);
    }

    static AiSchema top(AiSchema schema) {
        return schema.prop(TOP, AiSchema.integer("fin.ai.param.top").min(1).max(AiTool.MAX_ROWS), false);
    }

    static LocalDate start(AiToolArgs args) {
        LocalDate d = args.getDate(START_DATE);
        return d != null ? d : LocalDate.now().withDayOfMonth(1);
    }

    static LocalDate end(AiToolArgs args) {
        LocalDate d = args.getDate(END_DATE);
        return d != null ? d : LocalDate.now();
    }

    /** 会计期间 yyyyMM，默认本月 */
    static int period(AiToolArgs args) {
        YearMonth now = YearMonth.now();
        int p = args.getInt(PERIOD, now.getYear() * 100 + now.getMonthValue());
        if (p % 100 < 1 || p % 100 > 12) {
            throw new BizException("framework.error.aiInvalidToolArgument", PERIOD, String.valueOf(p));
        }
        return p;
    }

    static int top(AiToolArgs args) {
        return args.getInt(TOP, DEFAULT_TOP);
    }

    /** 关键字为空，或任一值包含关键字（不区分大小写） */
    static boolean matches(String keyword, String... values) {
        if (keyword == null) {
            return true;
        }
        String k = keyword.toLowerCase(Locale.ROOT);
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains(k)) {
                return true;
            }
        }
        return false;
    }

    /** 合计行的名称 */
    static String totalLabel() {
        return Messages.get("fin.text.total");
    }
}
