package com.shiwei.seckill.order.enums;

public enum OrderStatusEnum {
    PENDING_PAY(0, "待支付"),
    CANCELED(1, "已取消"),
    PAID(2, "已支付"),
    DELIVERED(3, "已发货"),
    SIGNED(4, "已签收"),
    AFTER_SALE(5, "售后中"),
    REFUNDED(6, "已退款");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + code);
    }
}
