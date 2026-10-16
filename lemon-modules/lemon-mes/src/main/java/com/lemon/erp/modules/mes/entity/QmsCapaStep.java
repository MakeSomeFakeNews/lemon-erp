package com.lemon.erp.modules.mes.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 8D 步骤：D1 组建团队、D2 问题描述、D3 临时措施、D4 根本原因、D5 永久措施、D6 实施验证、D7 预防再发、D8 总结表彰
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_capa_step")
public class QmsCapaStep extends BaseEntity {

    private Long capaId;

    /** D1 ~ D8 */
    private String step;

    private String content;

    private Long ownerId;

    private Integer done;

    private LocalDate doneDate;

    @TableField(exist = false)
    private String ownerName;
}
