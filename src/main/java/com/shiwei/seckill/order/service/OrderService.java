package com.shiwei.seckill.order.service;

import com.shiwei.seckill.order.model.OrderRecord;
import com.shiwei.seckill.order.model.OrderDetailUpdateReq;
import com.shiwei.seckill.order.model.OrderPageResult;
import com.shiwei.seckill.order.model.OrderSubmitReq;
import com.shiwei.seckill.order.entity.OrderEntity;

import java.util.List;

public interface OrderService {
    OrderRecord submit(OrderSubmitReq req);

    List<OrderRecord> list();

    OrderPageResult page(Long lastId, Integer size, String lastCreatedTime, Integer status);

    OrderRecord detail(Long orderId);

    OrderRecord detailByOrderNo(String orderNo);

    OrderRecord updateDetailOptions(Long orderId, OrderDetailUpdateReq req);

    OrderRecord cancel(Long orderId);

    OrderEntity getEntityById(Long orderId);

    OrderEntity getEntityByOrderNo(String orderNo);
}

