package com.shiwei.seckill.order.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.promotion.service.CouponService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@Component
public class OrderEventConsumer {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private CouponService couponService;

    @KafkaListener(topics = "order-event", groupId = "shiwei-order-event")
    public void consumeOrderEvent(String payload) {
        Map<String, Object> event = parse(payload);
        String eventType = stringValue(event.get("event"));
        if ("USER_CANCEL".equals(eventType) || "PAY_TIMEOUT".equals(eventType)) {
            Long userId = longValue(event.get("userId"));
            Long couponId = longValue(event.get("couponId"));
            if (userId != null && couponId != null) {
                couponService.restore(userId, couponId);
            }
        }
    }

    private Map<String, Object> parse(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("订单事件解析失败", e);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
