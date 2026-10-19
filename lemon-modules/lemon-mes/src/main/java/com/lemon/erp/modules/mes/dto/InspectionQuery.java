package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.InspectionStatus;
import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.QcType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * @param status 草稿 / 已确认（首页「检验单待确认」带 status=DRAFT）
 */
public record InspectionQuery(QcType type, QcResult result, InspectionStatus status, String keyword, Long supplierId,
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
