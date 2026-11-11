package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.NettingRequests;
import com.lemon.erp.modules.fin.entity.FinNetting;
import com.lemon.erp.modules.fin.entity.FinNettingItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinNettingMapper extends BaseMapper<FinNetting> {

    IPage<FinNetting> selectPageList(IPage<FinNetting> page, @Param("q") NettingRequests.Query query);

    FinNetting selectDetail(@Param("id") Long id);

    /** 核销明细（带单据号、日期、来源） */
    List<FinNettingItem> selectItems(@Param("nettingId") Long nettingId);

    void insertItems(@Param("list") List<FinNettingItem> items);
}
