package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinNoteLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinNoteLogMapper extends BaseMapper<FinNoteLog> {

    List<FinNoteLog> selectByNote(@Param("noteId") Long noteId);
}
