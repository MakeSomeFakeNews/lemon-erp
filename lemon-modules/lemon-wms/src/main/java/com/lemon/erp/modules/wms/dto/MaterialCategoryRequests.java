package com.lemon.erp.modules.wms.dto;

import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 物料分类的请求 */
public final class MaterialCategoryRequests {

    private MaterialCategoryRequests() {
    }

    /**
     * 新增 / 修改分类
     *
     * @param parentId 上级分类，为空或 0 为一级分类
     * @param code     分类编码，新增时为空自动生成；修改时忽略（编码保存后不能修改）
     * @param sort     同级排序，为空排在最后
     */
    public record Save(
            Long parentId,
            @Pattern(regexp = "^[A-Za-z0-9_-]{0,16}$", message = "{wms.valid.categoryCodeFormat}") String code,
            @NotBlank(message = "{wms.valid.categoryNameRequired}")
            @Size(max = 64, message = "{wms.valid.categoryNameTooLong}") String name,
            Integer sort,
            EnableStatus status,
            @Size(max = 255, message = "{wms.valid.categoryRemarkTooLong}") String remark) {
    }

    /**
     * 拖动排序：把 ids 依次排在 parentId 下（不在该上级下的分类移动过来），排序号按顺序重新编为 1、2、3…
     *
     * @param parentId 上级分类，为空或 0 为一级分类
     * @param ids      该上级下的分类，按新的顺序
     */
    public record Sort(Long parentId, @NotEmpty(message = "{wms.valid.categorySortIdsRequired}") List<Long> ids) {
    }
}
