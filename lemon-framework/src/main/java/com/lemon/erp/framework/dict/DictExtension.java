package com.lemon.erp.framework.dict;

import java.math.BigDecimal;

/**
 * 业务字典取值的扩展值（数字）：个别字典的每个取值需要附带一个数字时（如开票类型的税率），由使用它的业务模块声明为 Bean。
 * 字典页面据此多显示一列、保存时必填并按范围校验（存于 {@code sys_dict_data.ext_value}），业务代码用 {@link DictLookup#extValues} 读取；
 * 没有声明的字典不保存扩展值
 *
 * @param dictType 字典类型编码（业务字典）
 * @param label    扩展值名称（消息键，由声明的模块登记），如 税率(%)
 * @param scale    最多小数位数
 * @param min      最小值
 * @param max      最大值
 */
public record DictExtension(String dictType, String label, int scale, BigDecimal min, BigDecimal max) {
}
