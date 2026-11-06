package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ai.dto.AiImportMappingRequest;
import com.lemon.erp.modules.ai.dto.AiImportMappingResult;
import com.lemon.erp.modules.ai.service.AiImportMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Excel 导入的字段对应助手：登录即可调用（导入本身仍由各业务接口校验权限），需要「数据整理」功能已开启，用量记在数据整理下
 */
@RestController
@RequestMapping("/ai/import")
@RequiredArgsConstructor
public class AiImportController {

    private final AiImportMappingService mappingService;

    @PostMapping("/mapping")
    public R<AiImportMappingResult> mapping(@Valid @RequestBody AiImportMappingRequest req) {
        return R.ok(mappingService.map(req));
    }
}
