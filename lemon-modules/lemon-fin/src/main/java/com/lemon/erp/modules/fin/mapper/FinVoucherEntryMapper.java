package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.dto.AccountSum;
import com.lemon.erp.modules.fin.dto.AuxBalanceRow;
import com.lemon.erp.modules.fin.dto.LedgerRow;
import com.lemon.erp.modules.fin.entity.FinVoucherEntry;
import com.lemon.erp.modules.fin.model.AuxItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface FinVoucherEntryMapper extends BaseMapper<FinVoucherEntry> {

    /** 分录，带科目、部门名称 */
    List<FinVoucherEntry> selectByVoucherId(@Param("voucherId") Long voucherId);

    void insertBatch(@Param("list") List<FinVoucherEntry> list);

    /**
     * 已记账凭证的科目借贷发生额
     *
     * @param begin        起始期间（含），为空不限
     * @param end          截止期间（含），为空不限
     * @param excludeClose 是否排除结转损益凭证（利润表、预算执行）
     */
    List<AccountSum> sumByAccount(@Param("begin") Integer begin, @Param("end") Integer end,
                                  @Param("excludeClose") boolean excludeClose);

    /** 按科目、期间、部门汇总（预算执行），排除结转损益凭证 */
    List<AccountSum> sumByPeriodDept(@Param("begin") int begin, @Param("end") int end,
                                     @Param("accountIds") Collection<Long> accountIds);

    /** 明细账：科目（含下级）在期间范围内的已记账分录，按日期、凭证号排序 */
    List<LedgerRow> selectLedger(@Param("accountIds") Collection<Long> accountIds, @Param("begin") int begin, @Param("end") int end);

    /** 科目截至期末的余额（借 - 贷），含未记账凭证 */
    BigDecimal sumBalanceUpTo(@Param("accountId") Long accountId, @Param("period") int period);

    /** @param dimension 辅助核算项目 */
    List<AuxBalanceRow> selectAuxBalance(@Param("dimension") AuxItem dimension, @Param("begin") int begin, @Param("end") int end,
                                         @Param("accountId") Long accountId);
}
