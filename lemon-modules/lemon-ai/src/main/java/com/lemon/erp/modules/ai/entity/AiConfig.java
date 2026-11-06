package com.lemon.erp.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ai.model.AiFeature;
import com.lemon.erp.modules.ai.model.AiProvider;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 大模型配置，每个租户一条。API Key 只允许写入：查询时不返回（只返回脱敏后的提示），保存时为空表示不修改，操作日志中隐藏
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_config")
public class AiConfig extends BaseEntity {

    @NotNull(message = "{ai.valid.providerRequired}")
    private AiProvider provider;

    /** 接口地址（不含 /chat/completions），为空时使用服务商的默认地址 */
    @Size(max = 255, message = "{ai.valid.baseUrlTooLong}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String baseUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 255, message = "{ai.valid.apiKeyTooLong}")
    private String apiKey;

    /** 对话 / 工具调用使用的模型 */
    @Size(max = 64, message = "{ai.valid.modelTooLong}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String chatModel;

    /** 识图模型，为空时智能录入只支持 PDF、Excel、文本 */
    @Size(max = 64, message = "{ai.valid.modelTooLong}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String visionModel;

    /** 总开关 */
    private Boolean enabled;

    /** 每月 token 上限（输入 + 输出），为空不限 */
    @Min(value = 1, message = "{ai.valid.monthlyLimitInvalid}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long monthlyTokenLimit;

    private Boolean chatEnabled;

    private Boolean extractEnabled;

    private Boolean cleanseEnabled;

    private Boolean reportEnabled;

    @DecimalMin(value = "0", message = "{ai.valid.temperatureInvalid}")
    @DecimalMax(value = "2", message = "{ai.valid.temperatureInvalid}")
    private BigDecimal temperature;

    /** 单次回答的最大 token 数，为空时由服务商决定 */
    @Min(value = 1, message = "{ai.valid.maxTokensInvalid}")
    @Max(value = 65536, message = "{ai.valid.maxTokensInvalid}")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxTokens;

    /** 是否已保存 API Key（密钥本身不返回） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private boolean apiKeySet;

    /** 脱敏后的 API Key，如 sk-1****abcd */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    private String apiKeyMask;

    /** 功能开关是否开启（TEST 恒为开启） */
    public boolean featureEnabled(AiFeature feature) {
        return switch (feature) {
            case CHAT -> Boolean.TRUE.equals(chatEnabled);
            case EXTRACT -> Boolean.TRUE.equals(extractEnabled);
            case CLEANSE -> Boolean.TRUE.equals(cleanseEnabled);
            case REPORT -> Boolean.TRUE.equals(reportEnabled);
            case TEST -> true;
        };
    }
}
