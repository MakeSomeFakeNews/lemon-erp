package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.framework.ai.AiCleanseField;
import com.lemon.erp.modules.ai.model.AiCleanseKind;
import com.lemon.erp.modules.ai.model.AiSuggestionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据整理建议。疑似重复：每条待停用的记录一行（同组共用 groupNo，keep* 为建议保留的记录）；
 * 规范化：每个字段一行（原值 → 建议值）；缺失字段提示：只有字段，没有建议值
 */
@Data
@TableName("ai_cleanse_suggestion")
public class AiCleanseSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private AiCleanseKind kind;

    /** 疑似重复的组号（任务内从 1 开始） */
    private Integer groupNo;

    private Long recordId;

    private String recordCode;

    private String recordName;

    /** 规范化 / 缺失的字段，疑似重复为 null */
    private AiCleanseField field;

    /** 生成建议时的原值（采纳前检查记录是否已被修改） */
    private String oldValue;

    private String newValue;

    /** 疑似重复：建议保留的记录 */
    private Long keepRecordId;

    private String keepCode;

    private String keepName;

    /** 模型给出的理由（模型按发起人的语言输出） */
    private String reason;

    /** 置信度 0～100 */
    private Integer confidence;

    private AiSuggestionStatus status;

    /** 采纳 / 忽略的人和时间 */
    private Long handledBy;

    @TableField(exist = false)
    private String handledByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
