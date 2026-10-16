package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmChannelReport;
import com.lemon.erp.modules.scm.entity.ScmChannelReportItem;
import com.lemon.erp.modules.scm.entity.ScmPriceList;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScmChannelReportMapper extends BaseMapper<ScmChannelReport> {

    /** @param customerId 为空时全部经销商 */
    IPage<ScmChannelReport> selectPageList(IPage<ScmChannelReport> page, @Param("customerId") Long customerId, @Param("period") Integer period,
                                           @Param("keyword") String keyword);

    ScmChannelReport selectDetail(@Param("id") Long id);

    List<ScmChannelReportItem> selectItems(@Param("reportId") Long reportId);

    int deleteItems(@Param("reportId") Long reportId);

    int insertItems(@Param("reportId") Long reportId, @Param("list") List<ScmChannelReportItem> items);

    /** 经销商可订货的物料：价目表中当天有效、适用客户等级的价格（含起订数量阶梯），附物料编码、名称、规格、单位 */
    List<ScmPriceList> selectCatalog(@Param("level") String level, @Param("date") LocalDate date, @Param("keyword") String keyword);

    /** 用户所在部门（经销商订单记到负责销售的部门） */
    Long selectUserDept(@Param("userId") Long userId);
}
