package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ai.model.AiCleansePhase;
import com.lemon.erp.modules.ai.model.AiCleanseTaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 数据整理任务：按数据类型（AiCleanseSource）在后台查重、生成规范化建议，记录进度 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_cleanse_task")
public class AiCleanseTask extends BaseEntity {

    /** 数据类型（AiCleanseSource.key） */
    private String sourceKey;

    /** 编码或名称关键字（整理范围），为空表示全部 */
    private String keyword;

    private AiCleanseTaskStatus status;

    private AiCleansePhase phase;

    /** 进行中时为数据类型，结束后清空：唯一约束保证同一数据类型同时只有一个进行中的任务 */
    @JsonIgnore
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String runningKey;

    /** 读取的记录数 */
    private Integer recordCount;

    /** 记录超过上限，只分析了前 recordCount 条 */
    private Boolean truncated;

    /** 程序找出的疑似重复候选组数（交给模型判断的） */
    private Integer groupCount;

    /** 模型调用的总步数、已完成步数（进度） */
    private Integer totalSteps;

    private Integer doneSteps;

    /** 模型输出不合格而跳过的步数 */
    private Integer failedSteps;

    /** 生成的建议数：疑似重复（待停用的记录）、规范化、缺失字段提示 */
    private Integer duplicateCount;

    private Integer normalizeCount;

    private Integer missingCount;

    /** 模型给出但校验不通过而丢弃的建议数 */
    private Integer discardedCount;

    /** 消耗的 token（输入 + 输出） */
    private Integer tokens;

    /** 失败原因的消息键和参数（JSON 数组），返回时按用户语言生成 errorMessage */
    @JsonIgnore
    private String errorKey;

    @JsonIgnore
    private String errorArgs;

    @TableField(exist = false)
    private String errorMessage;

    /** 发起人（自动填充） */
    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    /** 数据类型名称（按用户语言） */
    @TableField(exist = false)
    private String sourceLabel;
}
