package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ai.dto.AiImportMappingRequest;
import com.lemon.erp.modules.ai.dto.AiImportMappingResult;
import com.lemon.erp.modules.ai.service.AiImportMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Excel 导入的字段对应助手：需要「数据整理」的查看权限（会消耗模型用量，不能只要求登录），并且数据整理功能已开启，用量记在数据整理下；
 * 导入本身仍由各业务接口校验权限
 */
@RestController
@RequestMapping("/ai/import")
@RequiredArgsConstructor
public class AiImportController {

    private final AiImportMappingService mappingService;

    @PostMapping("/mapping")
    @PreAuthorize("@ss.hasPerm('ai:cleanse:list')")
    public R<AiImportMappingResult> mapping(@Valid @RequestBody AiImportMappingRequest req) {
        return R.ok(mappingService.map(req));
    }
}
