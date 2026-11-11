package com.lemon.erp.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料（商品）图片：排序最小的为主图，主图 URL 同步到 {@link WmsMaterial#getImageUrl()}。
 * 文件存在公开文件目录（与头像相同，通过 /api/files/** 直接访问，文件名随机）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_material_image")
public class WmsMaterialImage extends BaseEntity {

    private Long materialId;

    /** 访问 URL，如 /api/files/material/2026/11/xxx.jpg */
    private String url;

    private String fileName;

    private Long fileSize;

    private Integer sort;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;
}
