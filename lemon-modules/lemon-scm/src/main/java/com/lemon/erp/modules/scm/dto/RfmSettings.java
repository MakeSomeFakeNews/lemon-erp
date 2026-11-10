package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.RfmMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * RFM 分群参数。作为查询参数时为空的项取租户的默认值（业务参数 scm.rfm.*），返回结果中是实际使用的值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RfmSettings {

    private RfmMode mode;
    /** R：最近订货在多少天之内为「高」（固定阈值） */
    private Integer recencyDays;
    /** F：每月订货不少于多少次为「高」（固定阈值） */
    private BigDecimal frequency;
    /** M：平均每次订货金额不少于多少元为「高」（固定阈值） */
    private BigDecimal monetary;
}
