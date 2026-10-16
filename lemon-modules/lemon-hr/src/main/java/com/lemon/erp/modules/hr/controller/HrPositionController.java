package com.lemon.erp.modules.hr.controller;

import com.lemon.erp.common.dto.StatusRequest;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.hr.entity.HrPosition;
import com.lemon.erp.modules.hr.service.HrPositionService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/hr/position")
@RequiredArgsConstructor
public class HrPositionController {

    private final HrPositionService positionService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('hr:position:list')")
    public R<PageResult<HrPosition>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) Integer status) {
        return R.ok(positionService.page(current, size, keyword, category, status));
    }

    /** 登录即可访问：员工、招聘需求选择职位 */
    @GetMapping("/options")
    public R<List<HrPosition>> options() {
        return R.ok(positionService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:position:list')")
    public R<HrPosition> get(@PathVariable Long id) {
        return R.ok(positionService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('hr:position:add')")
    @OperLog(title = "职位管理", type = OperType.INSERT)
    public R<Void> create(@Validated(Create.class) @RequestBody HrPosition position) {
        positionService.create(position);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('hr:position:edit')")
    @OperLog(title = "职位管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody HrPosition position) {
        positionService.update(id, position);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('hr:position:edit')")
    @OperLog(title = "职位管理-修改状态", type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        positionService.changeStatus(id, req.status());
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('hr:position:remove')")
    @OperLog(title = "职位管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        positionService.delete(ids);
        return R.ok();
    }
}
