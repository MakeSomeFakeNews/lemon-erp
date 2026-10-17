package com.lemon.erp.framework.ref;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 删除主数据前调用：任一模块报告有引用时拒绝删除
 */
@Component
@RequiredArgsConstructor
public class ReferenceGuard {

    private final ObjectProvider<ReferenceChecker> checkers;

    /**
     * @param label 提示中的名称（已按当前语言翻译），如「物料「M001 电阻」」；各模块返回的引用名称在这里翻译
     */
    public void checkDeletable(RefType type, Long id, String label) {
        for (ReferenceChecker checker : checkers) {
            String ref = checker.findReference(type, id);
            if (ref != null) {
                throw new BizException("{0}已被{1}使用，不能删除，可以停用", label, I18n.t(ref));
            }
        }
    }
}
