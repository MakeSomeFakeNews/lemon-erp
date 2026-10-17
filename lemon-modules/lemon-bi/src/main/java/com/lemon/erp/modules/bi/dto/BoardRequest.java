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
        @NotBlank(message = "{bi.valid.dashboardNameRequired}") @Size(max = 64, message = "{bi.valid.nameCannotExceedCharacters}") String name,
        @NotNull(message = "{bi.valid.addWidgets}") @Size(max = 20, message = "{bi.valid.noMoreWidgets}") List<@Valid @NotNull Widget> widgets,
        Integer shared,
        @Size(max = 255, message = "{bi.valid.descriptionCannotExceedCharacters}") String remark) {

    public record Widget(@NotNull(message = "{bi.valid.selectReport}") Long reportId,
                         @NotNull(message = "{bi.valid.selectWidth}") @Min(value = 6, message = "{bi.valid.widthMust}") @Max(value = 24, message = "{bi.valid.widthMust}") Integer span,
                         String chart) {
    }
}
