package com.lemon.erp.modules.fin.dto;

import com.lemon.erp.modules.fin.model.AssetCheckResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 资产盘点的请求
 */
public final class AssetCheckRequests {

    private AssetCheckRequests() {
    }

    /** 新建盘点：按部门、存放地点（包含）生成盘点清单，都为空时盘点全部未处置资产 */
    public record Create(
            @NotNull(message = "{fin.valid.selectCountDate}") LocalDate checkDate,
            Long deptId,
            @Size(max = 128) String location,
            @Size(max = 255) String remark) {
    }

    /** 录入盘点结果：位置变化时填写实际的部门、使用人、存放地点 */
    public record Record(
            @NotNull(message = "{fin.valid.selectCountResult}") AssetCheckResult result,
            Long actualDeptId,
            Long actualUserId,
            @Size(max = 128) String actualLocation,
            @Size(max = 255) String remark) {
    }

    /** 盘盈：账外资产，完成后在固定资产中登记卡片 */
    public record Gain(
            @NotBlank(message = "{fin.valid.enterAssetName}") @Size(max = 64) String name,
            @Size(max = 128) String location,
            @Size(max = 255) String remark) {
    }
}
