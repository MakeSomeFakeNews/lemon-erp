package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.InOutQuery;
import com.lemon.erp.modules.wms.dto.InOutRow;
import org.apache.ibatis.annotations.Param;

/**
 * 收发存汇总：按物料汇总期初、各类出入库、期末数量（按单据业务日期）
 */
public interface WmsInOutMapper {

    IPage<InOutRow> selectPage(IPage<InOutRow> page, @Param("q") InOutQuery query);
}
