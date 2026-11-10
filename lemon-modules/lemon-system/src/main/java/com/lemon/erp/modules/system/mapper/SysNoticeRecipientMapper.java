package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.system.dto.NoticeReadStat;
import com.lemon.erp.modules.system.dto.NoticeReader;
import com.lemon.erp.modules.system.entity.SysNoticeRecipient;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SysNoticeRecipientMapper extends BaseMapper<SysNoticeRecipient> {

    void insertBatch(@Param("noticeId") Long noticeId, @Param("userIds") Collection<Long> userIds);

    /**
     * 发布范围内的启用内部用户（不含门户账号）：两个参数都为空时为全员
     *
     * @param deptIds 指定部门时为部门及其全部下级部门，否则为空
     * @param roleIds 指定角色时的角色（只算启用的角色），否则为空
     */
    List<Long> selectScopeUsers(@Param("deptIds") Collection<Long> deptIds,
                                @Param("roleIds") Collection<Long> roleIds);

    /** 各公告的应读、已读人数 */
    List<NoticeReadStat> selectStats(@Param("noticeIds") Collection<Long> noticeIds);

    /**
     * 接收人名单
     *
     * @param read true 已读，false 未读，null 全部
     */
    IPage<NoticeReader> selectReaders(IPage<NoticeReader> page, @Param("noticeId") Long noticeId, @Param("read") Boolean read,
                                      @Param("keyword") String keyword);

    /** 标为已读，返回更新的行数（已读过的不再更新） */
    int markRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId, @Param("time") LocalDateTime time);
}
