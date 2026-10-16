package com.lemon.erp.modules.mes.dto;

import com.lemon.erp.modules.mes.model.QcResult;
import com.lemon.erp.modules.mes.model.QcType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record InspectionQuery(QcType type, QcResult result, String keyword, Long supplierId,
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
