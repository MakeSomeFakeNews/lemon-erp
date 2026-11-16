package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.CheckMaterialQuery;
import com.lemon.erp.modules.wms.dto.CheckMaterialView;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockView;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface WmsCheckMapper extends BaseMapper<WmsCheck> {

    /** 分页查询盘点单，带明细行数和差异行数 */
    IPage<WmsCheck> selectPageList(IPage<WmsCheck> page, @Param("q") DocQuery query);

    WmsCheck selectDetail(@Param("id") Long id);

    /** 选择盘点的物料（物料资料，可按库存变动等筛选），只含物料字段 */
    IPage<CheckMaterialView> selectMaterialPage(IPage<CheckMaterialView> page, @Param("q") CheckMaterialQuery query);

    /** 物料的当前库存、最近变动时间、最新进货 / 销售日期（仓库为空时为全部仓库） */
    List<CheckMaterialView> selectMaterialStats(@Param("warehouseId") Long warehouseId, @Param("ids") Collection<Long> ids);

    /** 物料在仓库中数量不为 0 的库存位置 */
    List<StockView> selectMaterialSlots(@Param("warehouseId") Long warehouseId, @Param("ids") Collection<Long> ids);
}
