package com.shiwei.seckill.order.enums;

public enum OutboxSendStatusEnum {
    PENDING(0),
    SENT(1),
    DEAD(2);

    private final int code;

    OutboxSendStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

