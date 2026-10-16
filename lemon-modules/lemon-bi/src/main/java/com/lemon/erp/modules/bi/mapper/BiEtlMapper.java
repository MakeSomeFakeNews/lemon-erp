package com.lemon.erp.modules.bi.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据仓库同步：从业务表查询（select*，列别名即事实表列名）后由 insertFacts 写入事实表
 */
public interface BiEtlMapper {

    void clearSales();

    List<Map<String, Object>> selectSales();

    void costSales();

    void profitSales();

    void clearPurchase();

    List<Map<String, Object>> selectPurchase();

    void clearInventory(@Param("today") LocalDate today);

    List<Map<String, Object>> selectInventory();

    void costInventory(@Param("today") LocalDate today);

    void clearProduction();

    List<Map<String, Object>> selectProduction(@Param("today") LocalDate today);

    void clearFinance();

    List<Map<String, Object>> selectFinance();

    void clearHr();

    /** 人力：一个月份的部门、岗位类别汇总 */
    List<Map<String, Object>> selectHr(@Param("period") int period, @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    void clearCrm();

    List<Map<String, Object>> selectCrm();

    void clearProject();

    List<Map<String, Object>> selectProject();

    /** 批量写入事实表，rows 中每行的值与 columns 顺序一致 */
    void insertFacts(@Param("table") String table, @Param("columns") List<String> columns,
                     @Param("rows") List<List<Object>> rows);

    Map<String, Object> countRows();
}
