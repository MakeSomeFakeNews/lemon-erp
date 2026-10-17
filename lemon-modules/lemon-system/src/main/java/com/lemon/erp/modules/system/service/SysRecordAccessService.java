package com.lemon.erp.modules.system.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
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
            throw new BizException("不支持的业务类型");
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
     */
    public void check(String bizType, Long bizId, boolean write, String what) {
        RecordType type = type(bizType);
        if (bizId == null) {
            throw new BizException("不支持的业务类型");
        }
        String perm = write ? type.editPerm() : type.viewPerm();
        if (Arrays.stream(perm.split(",")).map(String::trim).noneMatch(permissionService::hasPerm)) {
            throw new BizException("没有权限" + (write ? "修改" : "查看") + type.name() + "的" + what);
        }
        Supplier<Object> loader = () -> type.loader().apply(bizId);
        Object record = type.rule() == null || SecurityUtils.getDataScope().all()
                ? loader.get() : DataPermissionContext.execute(type.rule(), loader);
        if (record == null) {
            throw new BizException(type.name() + "不存在或没有权限");
        }
    }
}
