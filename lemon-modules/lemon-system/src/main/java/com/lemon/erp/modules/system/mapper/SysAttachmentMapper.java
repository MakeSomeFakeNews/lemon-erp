package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysAttachment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysAttachmentMapper extends BaseMapper<SysAttachment> {

    /** 单据的附件（含上传人），按上传时间排序 */
    List<SysAttachment> selectByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
