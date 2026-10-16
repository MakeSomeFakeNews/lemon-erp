package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 批次：物料首次以该批次号入库时登记
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_batch")
public class WmsBatch extends BaseEntity {

    private Long materialId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;
}
