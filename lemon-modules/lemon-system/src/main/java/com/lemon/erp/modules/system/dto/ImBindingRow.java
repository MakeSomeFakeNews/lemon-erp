package com.lemon.erp.modules.system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 第三方集成的成员绑定列表：系统用户及其在某个平台上绑定的成员（未绑定时成员字段为空） */
@Data
public class ImBindingRow {
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String deptName;
    private EnableStatus status;
    private Long bindingId;
    private String imUserId;
    private String imName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bindTime;
}
