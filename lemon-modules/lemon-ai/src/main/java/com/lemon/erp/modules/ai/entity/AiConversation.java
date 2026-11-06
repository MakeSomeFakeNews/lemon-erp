package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 问数的会话，只有本人能访问 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long userId;

    private String title;
}
