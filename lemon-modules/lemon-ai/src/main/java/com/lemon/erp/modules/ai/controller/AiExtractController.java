package com.lemon.erp.modules.ai.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ai.dto.AiExtractCandidate;
import com.lemon.erp.modules.ai.dto.AiExtractResult;
import com.lemon.erp.modules.ai.dto.AiExtractTargetView;
import com.lemon.erp.modules.ai.service.AiExtractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 智能录入：上传图片 / PDF / 表格或粘贴文字，识别成单据草稿（只返回预填数据，不保存单据）。
 * 除 ai:extract:use 外还需要目标自身的权限（一般是对应单据的新增权限），由 {@link AiExtractService} 检查
 */
@RestController
@RequestMapping("/ai/extract")
@RequiredArgsConstructor
@PreAuthorize("@ss.hasPerm('ai:extract:use')")
public class AiExtractController {

    private final AiExtractService extractService;

    /** 当前用户可用的目标 */
    @GetMapping("/targets")
    public R<List<AiExtractTargetView>> targets() {
        return R.ok(extractService.targets());
    }

    /** 判断内容最可能是哪种目标，返回候选供用户确认 */
    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<List<AiExtractCandidate>> detect(@RequestParam(required = false) String text,
                                              @RequestPart(required = false) List<MultipartFile> files) {
        return R.ok(extractService.detect(text, files));
    }

    /** 识别并返回预填数据 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<AiExtractResult> extract(@RequestParam(required = false) String target,
                                      @RequestParam(required = false) String text,
                                      @RequestPart(required = false) List<MultipartFile> files) {
        return R.ok(extractService.extract(target, text, files));
    }
}
