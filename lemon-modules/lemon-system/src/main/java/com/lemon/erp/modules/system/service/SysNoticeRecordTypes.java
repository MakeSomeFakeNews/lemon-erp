package com.lemon.erp.modules.system.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知公告的附件：查看权限为空（登录即可），由 loader 判断——有管理端查看权限的看全部，其他人只能看发给自己的有效公告；
 * 上传、删除需要新增或修改公告的权限
 */
@Component
@RequiredArgsConstructor
public class SysNoticeRecordTypes implements RecordTypeProvider {

    private final SysNoticeService noticeService;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("sys_notice", "system.label.notice", "", "sys:notice:add, sys:notice:edit", null,
                noticeService::loadForAttachment));
    }
}
