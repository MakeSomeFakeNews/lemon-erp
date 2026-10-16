package com.lemon.erp.modules.bi.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据同步日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bi_etl_log")
public class BiEtlLog extends BaseEntity {

    /** 触发方式 MANUAL手动 SCHEDULE定时 */
    private String triggerType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 状态(字典 bi_etl_status) RUNNING运行中 SUCCESS成功 FAILED失败 */
    private String status;

    /** 各事实表行数，如 sales=120,purchase=80 */
    private String rowCounts;

    /** 失败原因 */
    private String message;

    /** 操作人，定时任务为空 */
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}
