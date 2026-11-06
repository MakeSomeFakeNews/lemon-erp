package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.modules.ai.model.AiFeature;
import lombok.Data;

import java.time.LocalDateTime;

/** 大模型调用的 token 用量，每次调用一条 */
@Data
@TableName("ai_usage")
public class AiUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调用人，定时任务等没有登录用户时为 null */
    private Long userId;

    private AiFeature feature;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
