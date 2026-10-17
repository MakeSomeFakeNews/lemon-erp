package com.lemon.erp.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.system.model.JobRunStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务：任务由业务模块的 JobHandler 定义，这里保存调度设置和最近一次执行情况
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity {

    /** JobHandler.key() */
    private String jobKey;

    private String name;

    private String cron;

    private String params;

    /** 1 启用 0 停用 */
    private EnableStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRunTime;

    /** 最近一次执行的结果 */
    private JobRunStatus lastStatus;

    private String lastMessage;

    private String remark;
}
