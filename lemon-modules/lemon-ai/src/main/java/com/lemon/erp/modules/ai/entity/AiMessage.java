package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ai.model.AiMessageRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息：用户提问、模型回答（可能带工具调用请求）、工具结果。
 * toolCalls / toolResult 是大字段，消息历史接口不返回，工具结果按需单独读取
 */
@Data
@TableName("ai_message")
public class AiMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private AiMessageRole role;

    /** 用户提问、模型回答的文字；工具消息为空 */
    private String content;

    /** 工具消息：对应的工具调用 ID */
    private String toolCallId;

    /** 工具消息：工具名称 */
    private String toolName;

    /** 模型回答：请求调用的工具（JSON 数组 [{id, name, arguments}]） */
    private String toolCalls;

    /** 工具消息：回传给模型的结果 JSON */
    private String toolResult;

    /** 模型回答：本次回答（含各轮工具调用）消耗的 token */
    private Integer tokens;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
