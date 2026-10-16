package com.lemon.erp.modules.workflow.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.workflow.identity.FlowIdentityProvider;
import com.lemon.erp.framework.workflow.identity.FlowRole;
import com.lemon.erp.framework.workflow.identity.FlowUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 选择审批人、转交人时使用的用户、角色列表，登录即可访问（只返回启用的用户的基本信息），
 * 不要求系统管理中的用户、角色查看权限
 */
@RestController
@RequestMapping("/workflow/identity")
@RequiredArgsConstructor
public class WfIdentityController {

    private final FlowIdentityProvider identityProvider;

    @GetMapping("/users")
    public R<PageResult<FlowUser>> users(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String username) {
        return R.ok(identityProvider.searchUsers(username, current, size));
    }

    @GetMapping("/roles")
    public R<List<FlowRole>> roles() {
        return R.ok(identityProvider.listRoles());
    }
}
