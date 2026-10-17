package com.lemon.erp.modules.bi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 大屏投屏链接：车间、仓库的电视通过链接免登录查看指定的大屏（只读），可停用、设置有效期
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bi_screen_share")
public class BiScreenShare extends BaseEntity {

    /** 租户：免登录访问时按链接所属租户查询 */
    private Long tenantId;

    /** 名称，如 一车间电视 */
    private String name;

    /** 访问令牌（随机 32 位），链接中使用 */
    private String token;

    /** 大屏（ScreenKey，逗号分隔），多个时轮播 */
    private String screens;

    /** 轮播间隔（秒） */
    private Integer intervalSeconds;

    /** 生产车间、生产进度大屏只显示该工作中心，为空显示全部（修改时可清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long workCenterId;

    /** 有效期至，为空长期有效（修改时可清空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 状态 1启用 0停用 */
    private Integer status;

    /** 最近访问时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String workCenterName;
}
