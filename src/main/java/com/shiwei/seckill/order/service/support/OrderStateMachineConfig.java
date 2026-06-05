package com.shiwei.seckill.order.service.support;

import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Component
public class OrderStateMachineConfig {
    private final Map<OrderStatusEnum, Map<OrderEventEnum, OrderStatusEnum>> transitions;

    public OrderStateMachineConfig() {
        Map<OrderStatusEnum, Map<OrderEventEnum, OrderStatusEnum>> config = new EnumMap<>(OrderStatusEnum.class);

        Map<OrderEventEnum, OrderStatusEnum> pendingPay = new EnumMap<>(OrderEventEnum.class);
        pendingPay.put(OrderEventEnum.PAY_SUCCESS, OrderStatusEnum.PAID);
        pendingPay.put(OrderEventEnum.PAY_TIMEOUT, OrderStatusEnum.CANCELED);
        pendingPay.put(OrderEventEnum.USER_CANCEL, OrderStatusEnum.CANCELED);
        config.put(OrderStatusEnum.PENDING_PAY, Collections.unmodifiableMap(pendingPay));

        Map<OrderEventEnum, OrderStatusEnum> paid = new EnumMap<>(OrderEventEnum.class);
        paid.put(OrderEventEnum.DELIVER_GOODS, OrderStatusEnum.DELIVERED);
        config.put(OrderStatusEnum.PAID, Collections.unmodifiableMap(paid));

        Map<OrderEventEnum, OrderStatusEnum> delivered = new EnumMap<>(OrderEventEnum.class);
        delivered.put(OrderEventEnum.USER_SIGN, OrderStatusEnum.SIGNED);
        config.put(OrderStatusEnum.DELIVERED, Collections.unmodifiableMap(delivered));

        Map<OrderEventEnum, OrderStatusEnum> signed = new EnumMap<>(OrderEventEnum.class);
        signed.put(OrderEventEnum.APPLY_AFTER_SALE, OrderStatusEnum.AFTER_SALE);
        config.put(OrderStatusEnum.SIGNED, Collections.unmodifiableMap(signed));

        Map<OrderEventEnum, OrderStatusEnum> afterSale = new EnumMap<>(OrderEventEnum.class);
        afterSale.put(OrderEventEnum.REFUND_FINISH, OrderStatusEnum.REFUNDED);
        config.put(OrderStatusEnum.AFTER_SALE, Collections.unmodifiableMap(afterSale));

        this.transitions = Collections.unmodifiableMap(config);
    }

    public OrderStatusEnum getTargetStatus(OrderStatusEnum sourceStatus, OrderEventEnum event) {
        Map<OrderEventEnum, OrderStatusEnum> rules = transitions.get(sourceStatus);
        return rules == null ? null : rules.get(event);
    }
}
