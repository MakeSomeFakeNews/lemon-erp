package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.ref.MergeImpact;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 往来单位（客户 / 供应商）合并的请求与预览
 */
public final class PartnerMergeRequests {

    /** 一次最多合并的资料数 */
    public static final int MAX_SOURCES = 20;

    private PartnerMergeRequests() {
    }

    /**
     * @param targetId  保留的资料
     * @param sourceIds 被合并的资料（合并后停用、标记为已合并）
     */
    public record Merge(@NotNull(message = "{scm.valid.mergeTargetRequired}") Long targetId,
                        @NotEmpty(message = "{scm.valid.mergeSourcesRequired}")
                        @Size(max = MAX_SOURCES, message = "{scm.valid.mergeSourcesTooMany}") List<Long> sourceIds) {
    }

    /**
     * 资料概要
     *
     * @param linkedName 关联的客户（供应商资料）/ 关联的供应商（客户资料）：编码 名称
     */
    public record Brief(Long id, String code, String name, EnableStatus status, String linkedName) {
    }

    /**
     * 合并预览：影响范围按数据种类汇总（已翻译名称），只列出有引用的；合并后的关联（一对一）
     *
     * @param linkedName 合并后保留资料的关联单位（编码 名称），没有为 null
     */
    public record Preview(Brief target, List<Brief> sources, List<MergeImpact> impacts, String linkedName) {
    }
}
