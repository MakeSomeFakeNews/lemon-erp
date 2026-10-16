package com.lemon.erp.modules.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块标题 */
    private String title;

    /** 操作类型，对应字典 sys_oper_type */
    private Integer operType;

    /** 类名.方法名 */
    private String method;

    private String requestMethod;

    private String url;

    private String ip;

    /** IP 归属地 */
    private String location;

    /** 操作人用户名 */
    private String operator;

    private String params;

    private String result;

    /** 1 成功 0 失败 */
    private Integer status;

    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operTime;
}
