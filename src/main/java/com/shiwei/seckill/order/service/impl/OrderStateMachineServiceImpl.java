package com.shiwei.seckill.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.entity.OrderOutboxEntity;
import com.shiwei.seckill.order.entity.OrderStatusLogEntity;
import com.shiwei.seckill.order.enums.OperatorTypeEnum;
import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.enums.OutboxSendStatusEnum;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.mapper.OrderOutboxMapper;
import com.shiwei.seckill.order.mapper.OrderStatusLogMapper;
import com.shiwei.seckill.order.service.OrderStateMachineService;
import com.shiwei.seckill.order.service.support.OrderOperateContext;
import com.shiwei.seckill.order.service.support.OrderStateMachineConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderStateMachineServiceImpl implements OrderStateMachineService {
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;
    @Resource
    private OrderOutboxMapper orderOutboxMapper;
    @Resource
    private OrderStateMachineConfig orderStateMachineConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderStatusEnum fireEvent(OrderEntity order, OrderEventEnum event, OrderOperateContext context) {
        OrderStatusEnum sourceStatus = OrderStatusEnum.fromCode(order.getOrderStatus());
        OrderStatusEnum targetStatus = orderStateMachineConfig.getTargetStatus(sourceStatus, event);
        if (targetStatus == null) {
            throw new BizException("订单状态不允许执行该操作");
        }

        LocalDateTime operateTime = context != null && context.getOperateTime() != null ? context.getOperateTime() : LocalDateTime.now();
        String payChannel = context == null ? null : context.getPayChannel();
        LocalDateTime payTime = event == OrderEventEnum.PAY_SUCCESS ? operateTime : null;
        LocalDateTime canceledTime = (event == OrderEventEnum.PAY_TIMEOUT || event == OrderEventEnum.USER_CANCEL) ? operateTime : null;

        int updated = orderMapper.updateStatusWithVersion(
            order.getId(),
            sourceStatus.getCode(),
            targetStatus.getCode(),
            order.getVersion(),
            payChannel,
            payTime,
            canceledTime
        );
        if (updated != 1) {
            throw new BizException("订单状态已变更，请刷新后重试");
        }

        orderStatusLogMapper.insert(buildLog(order, sourceStatus, targetStatus, event, context, operateTime));
        orderOutboxMapper.insert(buildOutbox(order, sourceStatus, targetStatus, event, operateTime));
        order.setOrderStatus(targetStatus.getCode());
        order.setVersion(order.getVersion() + 1);
        return targetStatus;
    }

    private OrderStatusLogEntity buildLog(OrderEntity order, OrderStatusEnum sourceStatus, OrderStatusEnum targetStatus,
                                          OrderEventEnum event, OrderOperateContext context, LocalDateTime operateTime) {
        OrderStatusLogEntity log = new OrderStatusLogEntity();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setSourceStatus(sourceStatus.getCode());
        log.setTargetStatus(targetStatus.getCode());
        log.setEventCode(event.name());
        log.setOperatorType(context != null && context.getOperatorType() != null ? context.getOperatorType().name() : OperatorTypeEnum.SYSTEM.name());
        log.setOperatorId(context == null ? null : context.getOperatorId());
        log.setRemark(context == null ? null : context.getRemark());
        log.setCreatedAt(operateTime);
        return log;
    }

    private OrderOutboxEntity buildOutbox(OrderEntity order, OrderStatusEnum sourceStatus, OrderStatusEnum targetStatus,
                                          OrderEventEnum event, LocalDateTime operateTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("userId", order.getUserId());
        payload.put("couponId", order.getCouponId());
        payload.put("sourceStatus", sourceStatus.name());
        payload.put("targetStatus", targetStatus.name());
        payload.put("event", event.name());
        payload.put("occurredAt", operateTime.toString());

        OrderOutboxEntity outbox = new OrderOutboxEntity();
        outbox.setBizKey(order.getOrderNo());
        outbox.setEventType(event.name());
        outbox.setPayload(writePayload(payload));
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.getCode());
        outbox.setRetryCount(0);
        outbox.setCreatedAt(operateTime);
        outbox.setUpdatedAt(operateTime);
        return outbox;
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException("订单事件序列化失败");
        }
    }

    public void setOrderStateMachineConfig(OrderStateMachineConfig orderStateMachineConfig) {
        this.orderStateMachineConfig = orderStateMachineConfig;
    }
}
