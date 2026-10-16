package com.lemon.erp.modules.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String ip;

    /** IP 归属地 */
    private String location;

    private String browser;

    private String os;

    /** 1 成功 0 失败 */
    private Integer status;

    /** 提示信息，如「登录成功」「用户名或密码错误」 */
    private String msg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;
}
