package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinIoDocItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinIoDocItemMapper extends BaseMapper<FinIoDocItem> {

    int insertBatch(@Param("list") List<FinIoDocItem> items);
}
