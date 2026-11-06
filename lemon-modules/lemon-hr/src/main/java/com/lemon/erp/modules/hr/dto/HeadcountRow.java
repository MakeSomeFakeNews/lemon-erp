package com.lemon.erp.modules.hr.dto;

import lombok.Data;

/**
 * 人员结构的一组：当前在职、试用期人数，期间内入职、离职人数
 */
@Data
public class HeadcountRow {

    private String groupName;
    private Long active;
    private Long probation;
    private Long joined;
    private Long lefted;
}
