package com.lemon.erp.modules.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

import static com.lemon.erp.common.util.Decimals.nz;

/**
 * 社保方案：缴纳城市的社保、公积金基数上下限和各险种比例（%）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_social_scheme")
public class HrSocialScheme extends BaseEntity {

    @NotBlank(message = "{hr.valid.enterSchemeName}")
    @Size(max = 64, message = "{hr.valid.schemeNameCannotExceedCharacters}")
    private String name;

    @Size(max = 32, message = "{hr.valid.cityCannotExceedCharacters}")
    private String city;

    @DecimalMin(value = "0", message = "{hr.valid.baseCannotLess}")
    private BigDecimal baseMin;

    /** 0 表示不限 */
    @DecimalMin(value = "0", message = "{hr.valid.baseCannotLess}")
    private BigDecimal baseMax;

    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal pensionPersonal;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal pensionCompany;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal medicalPersonal;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal medicalCompany;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal unemploymentPersonal;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal unemploymentCompany;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal injuryCompany;

    @DecimalMin(value = "0", message = "{hr.valid.baseCannotLess}")
    private BigDecimal housingMin;

    /** 0 表示不限 */
    @DecimalMin(value = "0", message = "{hr.valid.baseCannotLess}")
    private BigDecimal housingMax;

    private Integer status;

    @Size(max = 255, message = "{hr.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 个人合计比例（%） */
    public BigDecimal personalRate() {
        return nz(pensionPersonal).add(nz(medicalPersonal)).add(nz(unemploymentPersonal));
    }

    /** 单位合计比例（%） */
    public BigDecimal companyRate() {
        return nz(pensionCompany).add(nz(medicalCompany)).add(nz(unemploymentCompany)).add(nz(injuryCompany));
    }

    /** 基数按上下限取值，上限为 0 不限 */
    public static BigDecimal clamp(BigDecimal base, BigDecimal min, BigDecimal max) {
        BigDecimal v = nz(base).max(nz(min));
        return max != null && max.signum() > 0 ? v.min(max) : v;
    }
}
