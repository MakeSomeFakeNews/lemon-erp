package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.TrainingStatus;

/**
 * 培训状态流转
 *
 * @param status 开始 ONGOING / 结束 FINISHED / 取消 CANCELED
 */
public record TrainingStatusRequest(TrainingStatus status) {
}
