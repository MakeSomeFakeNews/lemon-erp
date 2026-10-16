package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysMessage;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface SysMessageMapper extends BaseMapper<SysMessage> {

    void insertBatch(@Param("list") List<SysMessage> list);

    /** 已有相同去重键消息的用户 */
    List<Long> selectDedupUsers(@Param("userIds") Collection<Long> userIds, @Param("dedupKey") String dedupKey);

    /** 拥有某个权限标识的启用用户 */
    List<Long> selectUsersWithPerm(@Param("perm") String perm);

    /** 启用的超级管理员（角色编码 admin） */
    List<Long> selectAdminUsers();
}
