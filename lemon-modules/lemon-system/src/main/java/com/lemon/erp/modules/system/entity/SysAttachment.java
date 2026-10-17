package com.lemon.erp.modules.system.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务附件：按 业务类型 + 业务 ID 挂在单据上，文件存放在不公开的附件目录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_attachment")
public class SysAttachment extends BaseEntity {

    /** 业务类型 RecordType.code */
    private String bizType;

    private Long bizId;

    private String fileName;

    private Long fileSize;

    private String contentType;

    /** 相对附件目录的路径，不返回前端 */
    @JsonIgnore
    private String storagePath;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 上传人 */
    @TableField(exist = false)
    private String createByName;
}
