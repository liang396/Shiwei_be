package com.shiwei.seckill.order.enums;

public enum OrderSourceTypeEnum {
    NORMAL(0),
    SECKILL(1);

    private final int code;

    OrderSourceTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
