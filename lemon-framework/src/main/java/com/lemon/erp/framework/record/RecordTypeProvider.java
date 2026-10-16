package com.lemon.erp.framework.record;

import java.util.List;

/**
 * 业务模块登记自己的业务记录类型；附件、变更历史的存储和访问校验由系统模块统一实现
 */
public interface RecordTypeProvider {

    List<RecordType> recordTypes();
}
