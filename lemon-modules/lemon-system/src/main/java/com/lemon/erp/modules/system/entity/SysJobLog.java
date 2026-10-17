package com.lemon.erp.modules.system.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.system.model.JobRunStatus;
import com.lemon.erp.modules.system.model.JobTrigger;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_log")
public class SysJobLog extends BaseEntity {

    private String jobKey;

    private JobTrigger triggerType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    private Long costMs;

    private JobRunStatus status;

    private String message;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;
}
