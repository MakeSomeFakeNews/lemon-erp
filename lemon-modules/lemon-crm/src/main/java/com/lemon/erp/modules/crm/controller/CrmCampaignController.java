package com.lemon.erp.modules.crm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.crm.entity.CrmCampaign;
import com.lemon.erp.modules.crm.service.CrmCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 市场活动 /crm/campaign
 */
@RestController
@RequestMapping("/crm/campaign")
@RequiredArgsConstructor
public class CrmCampaignController {

    private final CrmCampaignService campaignService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('crm:campaign:list')")
    public R<PageResult<CrmCampaign>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) String status) {
        return R.ok(campaignService.page(current, size, keyword, type, status));
    }

    /** 登录即可访问：线索、商机选择来源活动 */
    @GetMapping("/options")
    public R<List<CrmCampaign>> options() {
        return R.ok(campaignService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:campaign:list')")
    public R<CrmCampaign> get(@PathVariable Long id) {
        return R.ok(campaignService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('crm:campaign:add')")
    @OperLog(title = "市场活动", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Void> create(@Validated(Create.class) @RequestBody CrmCampaign campaign) {
        campaignService.create(campaign);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('crm:campaign:edit')")
    @OperLog(title = "市场活动", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody CrmCampaign campaign) {
        campaignService.update(id, campaign);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('crm:campaign:remove')")
    @OperLog(title = "市场活动", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        campaignService.delete(ids);
        return R.ok();
    }
}
