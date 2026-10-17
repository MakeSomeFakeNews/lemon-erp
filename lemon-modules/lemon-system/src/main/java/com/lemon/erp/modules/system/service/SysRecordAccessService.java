package com.lemon.erp.modules.system.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.datascope.DataScopes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 业务记录（附件、变更历史）的访问校验：业务类型登记的功能权限 + 业务记录的数据权限
 */
@Service
@RequiredArgsConstructor
public class SysRecordAccessService {

    private final PermissionService permissionService;
    private final List<RecordTypeProvider> providers;

    private volatile Map<String, RecordType> types;

    public RecordType type(String code) {
        RecordType type = code == null ? null : typeMap().get(code);
        if (type == null) {
            throw new BizException("system.error.unsupportedBusinessType");
        }
        return type;
    }

    /** 全部业务类型（登记顺序） */
    public List<RecordType> types() {
        return List.copyOf(typeMap().values());
    }

    private Map<String, RecordType> typeMap() {
        if (types == null) {
            Map<String, RecordType> map = new LinkedHashMap<>();
            providers.forEach(p -> p.recordTypes().forEach(t -> map.put(t.code(), t)));
            types = map;
        }
        return types;
    }

    /**
     * @param write true 校验修改权限，false 校验查看权限
     * @param what  提示中访问内容（如「附件」）的消息键
     */
    public void check(String bizType, Long bizId, boolean write, String what) {
        RecordType type = type(bizType);
        if (bizId == null) {
            throw new BizException("system.error.unsupportedBusinessType");
        }
        String perm = write ? type.editPerm() : type.viewPerm();
        if (Arrays.stream(perm.split(",")).map(String::trim).noneMatch(permissionService::hasPerm)) {
            if (write) {
                throw new BizException("system.error.noPermissionModify", Messages.get(type.name()), Messages.get(what));
            }
            throw new BizException("system.error.noPermissionView", Messages.get(type.name()), Messages.get(what));
        }
        Supplier<Object> loader = () -> type.loader().apply(bizId);
        Object record = type.rule() == null ? loader.get() : DataScopes.load(type.rule(), loader);
        if (record == null) {
            throw new BizException("system.error.doesNotExistAccessDenied", Messages.get(type.name()));
        }
    }
}
