package com.shiwei.seckill.order.service;

import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.service.support.OrderOperateContext;

public interface OrderStateMachineService {
    OrderStatusEnum fireEvent(OrderEntity order, OrderEventEnum event, OrderOperateContext context);
}

