package com.lemon.erp.modules.bi.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.bi.model.ScreenKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新建 / 修改投屏链接
 *
 * @param screens         显示的大屏，多个时轮播
 * @param intervalSeconds 轮播间隔（秒）
 * @param workCenterId    生产车间、生产进度大屏只显示该工作中心，为空显示全部
 * @param expireTime      有效期至，为空长期有效
 * @param status          1 启用 0 停用
 */
public record ScreenShareRequest(@NotBlank @Size(max = 64) String name,
                                 @NotEmpty List<@NotNull ScreenKey> screens,
                                 @NotNull @Min(10) @Max(3600) Integer intervalSeconds,
                                 Long workCenterId,
                                 LocalDateTime expireTime,
                                 @NotNull EnableStatus status) {
}
