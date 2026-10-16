package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.QmsCapa;
import com.lemon.erp.modules.mes.model.CapaStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface QmsCapaMapper extends BaseMapper<QmsCapa> {

    IPage<QmsCapa> selectPageList(IPage<QmsCapa> page, @Param("status") CapaStatus status, @Param("keyword") String keyword);

    QmsCapa selectDetail(@Param("id") Long id);

    /** 超过要求完成日期仍未关闭的 */
    List<QmsCapa> selectOverdue(@Param("today") LocalDate today);
}
