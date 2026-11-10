package com.lemon.erp.modules.fin.mapper;

import com.lemon.erp.modules.fin.dto.FxRevaluation;
import com.lemon.erp.modules.fin.model.BillKind;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.AgingRow;
import com.lemon.erp.modules.fin.dto.BillQuery;
import com.lemon.erp.modules.fin.entity.FinBill;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FinBillMapper extends BaseMapper<FinBill> {

    IPage<FinBill> selectPageList(IPage<FinBill> page, @Param("q") BillQuery query, @Param("today") LocalDate today);

    /**
     * 账龄分析：未核销余额按到期日分段
     *
     * @param d30 asOf - 30 天，到期日早于它的逾期超过 30 天，依此类推
     */
    IPage<AgingRow> selectAging(IPage<AgingRow> page, @Param("kind") BillKind kind, @Param("asOf") LocalDate asOf,
                                @Param("d30") LocalDate d30, @Param("d60") LocalDate d60, @Param("d90") LocalDate d90);

    /** 核销（金额与单据同号），累计不超过单据金额；返回 0 表示超出 */
    int addSettled(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 累加原币已核销 */
    int addSettledFc(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 截至日期的外币单据未核销余额（期末调汇） */
    java.util.List<FxRevaluation.OpenBill> selectOpenForeign(@Param("asOf") java.time.LocalDate asOf);

    /** 累加收付款折让金额 */
    int addDiscount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 累加坏账核销金额 */
    int addBadDebt(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 发票勾稽：累加已开票 / 已收票金额，累计不超过单据金额；返回 0 表示超出 */
    int addInvoiced(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 来源出入库单按物料分类汇总的不含税金额、税额（生成应付 / 应收凭证）
     *
     * @return [{category, net, tax}]
     */
    /** 订单关闭尾差调整的不含税金额（本位币）按物料分类汇总：[{category, net}]，合计等于调整单的不含税金额 */
    List<Map<String, Object>> selectOrderAdjustByCategory(@Param("orderId") Long orderId);

    List<Map<String, Object>> selectSourceByCategory(@Param("tradeDocId") Long tradeDocId);

    /** 采购入库单分摊的运费按物料分类汇总：[{category, net}] */
    List<Map<String, Object>> selectFreightByCategory(@Param("tradeDocId") Long tradeDocId);

    /** 往来单位未核销金额合计 */
    BigDecimal selectUnsettledAmount(@Param("kind") BillKind kind, @Param("partnerId") Long partnerId);
}
