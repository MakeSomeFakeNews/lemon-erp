package com.lemon.erp.framework.record;

import com.lemon.erp.framework.security.datascope.DataPermissionRule;

import java.util.function.LongFunction;

/**
 * 业务记录类型：可挂附件、记录字段变更历史的单据或主数据，由业务模块通过 {@link RecordTypeProvider} 登记。
 * 附件、变更历史按这里的权限和数据权限校验访问
 *
 * @param code     类型编码，如 scm_purchase，前端 AttachmentPanel / ChangeLogPanel 的 biz-type
 * @param name     名称
 * @param viewPerm 查看附件、变更历史需要的权限，多个用逗号分隔（有其一即可，如内部与供应商门户共用的单据）
 * @param editPerm 上传、删除附件需要的权限，多个用逗号分隔
 * @param rule     业务记录的数据权限规则，为空不按数据权限过滤
 * @param loader   按 ID 读取业务记录（在数据权限规则下执行），返回 null 表示不存在或无权访问
 */
public record RecordType(String code, String name, String viewPerm, String editPerm,
                         DataPermissionRule rule, LongFunction<Object> loader) {
}
