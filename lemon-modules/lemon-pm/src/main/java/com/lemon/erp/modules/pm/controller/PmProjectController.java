package com.lemon.erp.modules.pm.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.pm.dto.MemberRequest;
import com.lemon.erp.modules.pm.dto.ProjectRequest;
import com.lemon.erp.modules.pm.dto.ResourceRow;
import com.lemon.erp.modules.pm.entity.PmMember;
import com.lemon.erp.modules.pm.entity.PmProject;
import com.lemon.erp.modules.pm.service.PmMemberService;
import com.lemon.erp.modules.pm.service.PmProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Map;

/**
 * 项目 /pm/project、项目成员 /pm/project/{id}/member、资源负荷 /pm/resource
 */
@RestController
@RequestMapping("/pm")
@RequiredArgsConstructor
public class PmProjectController {

    private final PmProjectService projectService;
    private final PmMemberService memberService;

    @GetMapping("/project")
    @PreAuthorize("@ss.hasPerm('pm:project:list')")
    public R<PageResult<PmProject>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) Long customerId,
                                         @RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(projectService.page(current, size, keyword, status, type, mine, customerId));
    }

    /** 登录即可访问：选择项目；mine 只返回我参与的（填报工时） */
    @GetMapping("/project/options")
    public R<List<PmProject>> options(@RequestParam(defaultValue = "false") boolean mine) {
        return R.ok(projectService.options(mine));
    }

    @GetMapping("/project/{id}")
    @PreAuthorize("@ss.hasAnyPerm('pm:project:list', 'pm:task:list', 'pm:cost:list', 'pm:accounting:list')")
    public R<PmProject> get(@PathVariable Long id) {
        return R.ok(projectService.detail(id));
    }

    @PostMapping("/project")
    @PreAuthorize("@ss.hasPerm('pm:project:add')")
    @OperLog(title = "项目管理", type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody ProjectRequest req) {
        return R.ok(projectService.create(req));
    }

    @PutMapping("/project/{id}")
    @PreAuthorize("@ss.hasPerm('pm:project:edit')")
    @OperLog(title = "项目管理", type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        projectService.update(id, req);
        return R.ok();
    }

    /** 状态：ACTIVE 启动 / 恢复，SUSPENDED 暂停，FINISHED 完成，CLOSED 关闭；body：{ status }。返回 true 表示已提交立项审批 */
    @PutMapping("/project/{id}/status")
    @PreAuthorize("@ss.hasPerm('pm:project:edit')")
    @OperLog(title = "项目管理-状态", type = OperType.UPDATE)
    public R<Boolean> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(projectService.changeStatus(id, body.get("status")));
    }

    @DeleteMapping("/project/{ids}")
    @PreAuthorize("@ss.hasPerm('pm:project:remove')")
    @OperLog(title = "项目管理", type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        projectService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 成员与资源

    @GetMapping("/project/{id}/member")
    @PreAuthorize("@ss.hasAnyPerm('pm:member:list', 'pm:project:list')")
    public R<List<PmMember>> members(@PathVariable Long id) {
        return R.ok(memberService.list(id));
    }

    @PostMapping("/project/{id}/member")
    @PreAuthorize("@ss.hasPerm('pm:member:edit')")
    @OperLog(title = "项目成员", type = OperType.INSERT)
    public R<Void> addMember(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        projectService.get(id);
        memberService.save(id, null, req);
        return R.ok();
    }

    @PutMapping("/project/{id}/member/{memberId}")
    @PreAuthorize("@ss.hasPerm('pm:member:edit')")
    @OperLog(title = "项目成员", type = OperType.UPDATE)
    public R<Void> updateMember(@PathVariable Long id, @PathVariable Long memberId, @Valid @RequestBody MemberRequest req) {
        memberService.save(id, memberId, req);
        return R.ok();
    }

    @DeleteMapping("/project/{id}/member/{memberId}")
    @PreAuthorize("@ss.hasPerm('pm:member:edit')")
    @OperLog(title = "项目成员", type = OperType.DELETE)
    public R<Void> deleteMember(@PathVariable Long id, @PathVariable Long memberId) {
        memberService.delete(id, memberId);
        return R.ok();
    }

    @GetMapping("/resource")
    @PreAuthorize("@ss.hasPerm('pm:member:list')")
    public R<List<ResourceRow>> resources(@RequestParam(required = false) Long deptId) {
        return R.ok(memberService.resources(deptId));
    }
}
