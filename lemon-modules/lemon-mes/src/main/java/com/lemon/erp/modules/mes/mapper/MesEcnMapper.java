package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesEcn;
import com.lemon.erp.modules.mes.model.EcnStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface MesEcnMapper extends BaseMapper<MesEcn> {

    IPage<MesEcn> selectPageList(IPage<MesEcn> page, @Param("status") EcnStatus status, @Param("keyword") String keyword);

    MesEcn selectDetail(@Param("id") Long id);

    /** 已批准且到了生效日期的 */
    List<MesEcn> selectDue(@Param("today") LocalDate today);

    int transit(@Param("id") Long id, @Param("from") EcnStatus from, @Param("to") EcnStatus to);
}
