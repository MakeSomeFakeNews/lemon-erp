package com.lemon.erp.modules.scm.model;

/**
 * 商品速查中显示的价格（用户在个人设置 quickLookup 中选择一项，价格均为含税、基本单位、本位币）。
 * 能否看到按权限决定，与价格参考一致：进货类（参考进货价、最新进价）需要采购订单 / 采购入库的查看权限，
 * 库存均价需要「查看成本金额」（wms:stock:cost），售价类（最新售价、零售价、批发价）需要报价 / 销售订单 / 销售出库的查看权限
 * 或「商品价格」页面权限；零售价、批发价只有启用的价格列可选
 */
public enum QuickPrice {
    /** 参考进货价：物料的参考成本价 */
    REF_COST(Side.PURCHASE, null),
    /** 库存均价：移动加权平均成本 */
    AVG_COST(Side.COST, null),
    /** 最新进价：最近一次采购入库的单价（优惠前，按数据权限） */
    LAST_PURCHASE(Side.PURCHASE, null),
    /** 最新售价：最近一次销售出库的单价（优惠前，按数据权限） */
    LAST_SALES(Side.SALES, null),
    RETAIL(Side.SALES, 0),
    WHOLESALE1(Side.SALES, 1),
    WHOLESALE2(Side.SALES, 2),
    WHOLESALE3(Side.SALES, 3),
    WHOLESALE4(Side.SALES, 4),
    WHOLESALE5(Side.SALES, 5),
    WHOLESALE6(Side.SALES, 6),
    WHOLESALE7(Side.SALES, 7),
    WHOLESALE8(Side.SALES, 8),
    /** 不显示价格，只显示数量 */
    NONE(null, null);

    /** 价格所属的方面，决定需要的权限 */
    public enum Side { PURCHASE, COST, SALES }

    private final Side side;
    private final Integer level;

    QuickPrice(Side side, Integer level) {
        this.side = side;
        this.level = level;
    }

    /** 所属方面，NONE 为 null */
    public Side side() {
        return side;
    }

    /** 物料价格列（0 零售价，1-8 批发价），不是价格列时为 null */
    public Integer level() {
        return level;
    }
}
