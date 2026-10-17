package com.lemon.erp.framework.setting;

/**
 * 下拉选项类参数的取值枚举：按名称保存，label 是显示在参数设置页面的名称的消息键（由声明它的模块登记），输出时 {@code Messages.get}
 */
public interface OptionEnum {

    String name();

    String label();
}
