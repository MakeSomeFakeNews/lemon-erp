package com.lemon.erp.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.record.ChangeLogger;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.system.entity.SysChangeLog;
import com.lemon.erp.modules.system.mapper.SysChangeLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 字段变更历史：记录有变化的字段，按业务类型的权限和数据权限查看
 */
@Service
@RequiredArgsConstructor
public class SysChangeLogService implements ChangeLogger {

    private static final int MAX_VALUE_LENGTH = 500;

    private final SysChangeLogMapper changeLogMapper;
    private final SysRecordAccessService recordAccessService;

    @Override
    public void log(String bizType, Long bizId, Object before, Object after, Map<String, String> fields) {
        if (before == null || after == null || bizId == null) {
            return;
        }
        Long userId = SecurityUtils.getUserIdOrNull();
        List<SysChangeLog> logs = new ArrayList<>();
        fields.forEach((field, label) -> {
            String oldValue = text(read(before, field));
            String newValue = text(read(after, field));
            if (!Objects.equals(oldValue, newValue)) {
                SysChangeLog l = new SysChangeLog();
                l.setBizType(bizType);
                l.setBizId(bizId);
                l.setFieldName(field);
                l.setFieldLabel(label);
                l.setOldValue(oldValue);
                l.setNewValue(newValue);
                l.setCreateBy(userId);
                logs.add(l);
            }
        });
        if (!logs.isEmpty()) {
            changeLogMapper.insertBatch(logs);
        }
    }

    public PageResult<SysChangeLog> page(String bizType, Long bizId, long current, long size) {
        recordAccessService.check(bizType, bizId, false, "system.label.changeHistory2");
        Page<SysChangeLog> page = Page.of(current, size);
        changeLogMapper.selectByBiz(page, bizType, bizId);
        return PageResult.of(page);
    }

    /** getter（getX / isX）或 record 访问器 x() */
    private static Object read(Object bean, String field) {
        String cap = Character.toUpperCase(field.charAt(0)) + field.substring(1);
        for (String name : new String[]{"get" + cap, "is" + cap, field}) {
            try {
                Method m = bean.getClass().getMethod(name);
                return m.invoke(bean);
            } catch (NoSuchMethodException ignored) {
                // 尝试下一种
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("读取字段失败：" + field, e); // i18n-ignore 内部错误
            }
        }
        throw new IllegalArgumentException(bean.getClass().getSimpleName() + " 没有字段 " + field); // i18n-ignore 内部错误
    }

    /** 统一格式：空串视为空，金额去掉末尾的 0 */
    private static String text(Object v) {
        if (v == null) {
            return null;
        }
        String s = v instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : v.toString();
        if (s.isBlank()) {
            return null;
        }
        return s.length() > MAX_VALUE_LENGTH ? s.substring(0, MAX_VALUE_LENGTH) : s;
    }
}
