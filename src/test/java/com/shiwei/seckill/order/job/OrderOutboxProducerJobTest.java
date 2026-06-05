package com.shiwei.seckill.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.order.entity.MessageDeadLetterEntity;
import com.shiwei.seckill.order.entity.OrderOutboxEntity;
import com.shiwei.seckill.order.enums.OutboxSendStatusEnum;
import com.shiwei.seckill.order.mapper.MessageDeadLetterMapper;
import com.shiwei.seckill.order.mapper.OrderOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOutboxProducerJobTest {
    @Mock
    private OrderOutboxMapper orderOutboxMapper;
    @Mock
    private MessageDeadLetterMapper messageDeadLetterMapper;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OrderOutboxProducerJob orderOutboxProducerJob;

    @Test
    void shouldSendPendingOutboxAndMarkSent() {
        OrderOutboxEntity outbox = new OrderOutboxEntity();
        outbox.setId(10L);
        outbox.setBizKey("SW123");
        outbox.setEventType("PAY_SUCCESS");
        outbox.setPayload("{\"orderNo\":\"SW123\"}");
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.getCode());
        outbox.setRetryCount(0);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());

        when(orderOutboxMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(outbox));

        orderOutboxProducerJob.publishPendingEvents();

        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
        ArgumentCaptor<OrderOutboxEntity> entityCaptor = ArgumentCaptor.forClass(OrderOutboxEntity.class);
        verify(orderOutboxMapper).updateById(entityCaptor.capture());
        assertEquals(OutboxSendStatusEnum.SENT.getCode(), entityCaptor.getValue().getSendStatus());
    }

    @Test
    void shouldIncreaseRetryCountWhenKafkaSendFails() {
        OrderOutboxEntity outbox = new OrderOutboxEntity();
        outbox.setId(10L);
        outbox.setBizKey("SW123");
        outbox.setEventType("USER_CANCEL");
        outbox.setPayload("{\"orderNo\":\"SW123\"}");
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.getCode());
        outbox.setRetryCount(1);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());

        when(orderOutboxMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(outbox));
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        orderOutboxProducerJob.publishPendingEvents();

        ArgumentCaptor<OrderOutboxEntity> entityCaptor = ArgumentCaptor.forClass(OrderOutboxEntity.class);
        verify(orderOutboxMapper).updateById(entityCaptor.capture());
        assertEquals(2, entityCaptor.getValue().getRetryCount().intValue());
        assertEquals(OutboxSendStatusEnum.PENDING.getCode(), entityCaptor.getValue().getSendStatus());
    }

    @Test
    void shouldBatchProcessMultiplePendingEvents() {
        OrderOutboxEntity first = buildOutbox(10L, "SW123", "PAY_SUCCESS");
        OrderOutboxEntity second = buildOutbox(11L, "SW124", "USER_CANCEL");
        List<OrderOutboxEntity> batch = Arrays.asList(first, second);

        when(orderOutboxMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(batch);

        orderOutboxProducerJob.publishPendingEvents();

        verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());
        verify(orderOutboxMapper).updateById(first);
        verify(orderOutboxMapper).updateById(second);
    }

    @Test
    void shouldMoveToDeadLetterAfterThreeFailures() {
        OrderOutboxEntity outbox = buildOutbox(12L, "SW125", "USER_CANCEL");
        outbox.setRetryCount(2);
        when(orderOutboxMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(outbox));
        doThrow(new RuntimeException("kafka down")).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        orderOutboxProducerJob.publishPendingEvents();

        ArgumentCaptor<OrderOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(OrderOutboxEntity.class);
        verify(orderOutboxMapper).updateById(outboxCaptor.capture());
        assertEquals(3, outboxCaptor.getValue().getRetryCount().intValue());
        assertEquals(OutboxSendStatusEnum.DEAD.getCode(), outboxCaptor.getValue().getSendStatus());

        ArgumentCaptor<MessageDeadLetterEntity> deadCaptor = ArgumentCaptor.forClass(MessageDeadLetterEntity.class);
        verify(messageDeadLetterMapper).insert(deadCaptor.capture());
        assertEquals("SW125", deadCaptor.getValue().getBizKey());
    }

    private OrderOutboxEntity buildOutbox(Long id, String bizKey, String eventType) {
        OrderOutboxEntity outbox = new OrderOutboxEntity();
        outbox.setId(id);
        outbox.setBizKey(bizKey);
        outbox.setEventType(eventType);
        outbox.setPayload("{\"orderNo\":\"" + bizKey + "\"}");
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.getCode());
        outbox.setRetryCount(0);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setUpdatedAt(LocalDateTime.now());
        return outbox;
    }
}
