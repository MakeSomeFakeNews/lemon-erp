package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.system.entity.SysNotice;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface SysNoticeMapper extends BaseMapper<SysNotice> {

    /**
     * 发布给本人、已发布且未过期的公告（不含正文），置顶在前、按发布时间倒序
     *
     * @param unread  true 只查未读
     * @param keyword 标题，模糊匹配
     */
    IPage<SysNotice> selectMine(IPage<SysNotice> page, @Param("userId") Long userId, @Param("today") LocalDate today,
                                @Param("unread") boolean unread, @Param("keyword") String keyword, @Param("type") String type);

    /** 本人未读的有效公告数 */
    long countMineUnread(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** 本人可见的一条公告（含正文），不可见时为空 */
    SysNotice selectMineById(@Param("id") Long id, @Param("userId") Long userId, @Param("today") LocalDate today);
}
