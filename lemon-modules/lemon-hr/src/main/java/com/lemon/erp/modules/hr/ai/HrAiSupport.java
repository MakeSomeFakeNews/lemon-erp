package com.lemon.erp.modules.hr.ai;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiToolArgs;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 人事模块 AI 问数工具的公共参数：日期范围（默认本月 1 日至今天）、月份（yyyyMM）
 */
final class HrAiSupport {

    static final String START_DATE = "startDate";
    static final String END_DATE = "endDate";
    static final String PERIOD = "period";
    static final String GROUP_BY = "groupBy";

    private HrAiSupport() {
    }

    static AiSchema dateRange(AiSchema schema) {
        return schema.prop(START_DATE, AiSchema.date("hr.ai.param.startDate"), false)
                .prop(END_DATE, AiSchema.date("hr.ai.param.endDate"), false);
    }

    static AiSchema period(AiSchema schema, String descriptionKey) {
        return schema.prop(PERIOD, AiSchema.integer(descriptionKey).min(200001).max(209912), false);
    }

    static LocalDate start(AiToolArgs args) {
        LocalDate d = args.getDate(START_DATE);
        return d != null ? d : LocalDate.now().withDayOfMonth(1);
    }

    static LocalDate end(AiToolArgs args) {
        LocalDate d = args.getDate(END_DATE);
        return d != null ? d : LocalDate.now();
    }

    /** 月份 yyyyMM，未传时为 defaultMonth */
    static int period(AiToolArgs args, YearMonth defaultMonth) {
        int p = args.getInt(PERIOD, defaultMonth.getYear() * 100 + defaultMonth.getMonthValue());
        if (p % 100 < 1 || p % 100 > 12) {
            throw new BizException("framework.error.aiInvalidToolArgument", PERIOD, String.valueOf(p));
        }
        return p;
    }

    static <E extends Enum<E>> E enumArg(AiToolArgs args, String name, Class<E> type, E defaultValue) {
        return args.has(name) ? Enum.valueOf(type, args.getString(name)) : defaultValue;
    }

    static String totalLabel() {
        return Messages.get("hr.ai.text.total");
    }
}
