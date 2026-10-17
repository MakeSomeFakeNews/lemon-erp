package com.lemon.erp.modules.mes.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.mes.model.DocStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 技术文档：同一文档的各版本 doc_no 相同，文件存于附件（业务类型 mes_doc）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_tech_doc")
public class MesTechDoc extends BaseEntity {

    private String docNo;

    private String name;

    /** 文档类型(业务字典 mes_doc_type) */
    private String docType;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long materialId;

    private String version;

    private DocStatus status;

    private Long prevId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long ecnId;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    private Long releaseBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime releaseTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String ecnNo;
    @TableField(exist = false)
    private String releaseByName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private Integer fileCount;
}
