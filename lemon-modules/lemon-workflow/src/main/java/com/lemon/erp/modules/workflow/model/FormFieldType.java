package com.lemon.erp.modules.workflow.model;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.exception.BizException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 表单字段类型，负责把前端提交的值校验并转换为流程变量
 */
public enum FormFieldType {

    INPUT {
        @Override
        Object convert(FormField field, Object raw) {
            return text(field, raw, 200);
        }
    },
    TEXTAREA {
        @Override
        Object convert(FormField field, Object raw) {
            return text(field, raw, 2000);
        }
    },
    /** 整数保存为 Long，小数保存为 Double */
    NUMBER {
        @Override
        Object convert(FormField field, Object raw) {
            BigDecimal number;
            try {
                number = new BigDecimal(raw.toString().trim());
            } catch (NumberFormatException e) {
                throw invalid(field, Messages.get("workflow.text.mustNumber"));
            }
            if (number.abs().compareTo(MAX_NUMBER) > 0) {
                throw invalid(field, Messages.get("workflow.text.outRange"));
            }
            BigDecimal stripped = number.stripTrailingZeros();
            return stripped.scale() <= 0 ? (Object) stripped.longValueExact() : (Object) number.doubleValue();
        }
    },
    /** yyyy-MM-dd */
    DATE {
        @Override
        Object convert(FormField field, Object raw) {
            String value = raw.toString();
            try {
                LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                throw invalid(field, Messages.get("workflow.text.hasInvalidDateFormat"));
            }
            return value;
        }
    },
    SELECT {
        @Override
        Object convert(FormField field, Object raw) {
            String value = raw.toString();
            if (!field.options().contains(value)) {
                throw invalid(field, Messages.get("workflow.text.notAllowedValue"));
            }
            return value;
        }
    };

    private static final BigDecimal MAX_NUMBER = new BigDecimal("1000000000000");

    /**
     * 校验并转换字段值
     *
     * @return 转换后的值；未填写时返回 null（必填字段未填写会抛出异常）
     */
    public Object parse(FormField field, Object raw) {
        if (raw == null || raw.toString().isBlank()) {
            if (field.isMandatory()) {
                throw new BizException("workflow.error.fill", field.label());
            }
            return null;
        }
        if (!(raw instanceof String || raw instanceof Number)) {
            throw invalid(field, Messages.get("workflow.text.hasInvalidFormat"));
        }
        return convert(field, raw);
    }

    abstract Object convert(FormField field, Object raw);

    private static String text(FormField field, Object raw, int maxLength) {
        String value = raw.toString().trim();
        if (value.length() > maxLength) {
            throw invalid(field, Messages.get("workflow.text.cannotExceedCharacters", maxLength));
        }
        return value;
    }

    private static BizException invalid(FormField field, String reason) {
        return new BizException("workflow.error.fieldInvalid", field.label(), reason);
    }
}
