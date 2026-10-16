package com.lemon.erp.modules.bi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 保存自定义仪表盘：由已保存的报表组成
 *
 * @param widgets 组件：报表、宽度（栅格 6-24）、图表类型（为空使用报表的图表类型）
 */
public record BoardRequest(
        @NotBlank(message = "仪表盘名称不能为空") @Size(max = 64, message = "名称不能超过64个字符") String name,
        @NotNull(message = "请添加组件") @Size(max = 20, message = "组件不能超过20个") List<@Valid @NotNull Widget> widgets,
        Integer shared,
        @Size(max = 255, message = "说明不能超过255个字符") String remark) {

    public record Widget(@NotNull(message = "请选择报表") Long reportId,
                         @NotNull(message = "请选择宽度") @Min(value = 6, message = "宽度为6-24") @Max(value = 24, message = "宽度为6-24") Integer span,
                         String chart) {
    }
}
