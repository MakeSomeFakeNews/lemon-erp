package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.framework.code.DocNoCatalog.DateFormat;
import com.lemon.erp.framework.code.DocNoCatalog.ResetCycle;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 编码规则：租户修改过的单据编号规则，未修改的单据使用 DocNoCatalog 中的默认规则
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_code_rule")
public class CodeRule extends BaseEntity {

    /** 规则标识（DocNoCatalog.Type.key） */
    private String ruleKey;

    private String prefix;

    private DateFormat dateFormat;

    private Integer seqLength;

    private ResetCycle resetCycle;
}
