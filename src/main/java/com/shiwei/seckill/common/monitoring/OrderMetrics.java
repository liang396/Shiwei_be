package com.shiwei.seckill.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {
    private final Counter orderSubmitCounter;
    private final Counter orderCancelCounter;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.orderSubmitCounter = meterRegistry.counter("shiwei.order.submit.count");
        this.orderCancelCounter = meterRegistry.counter("shiwei.order.cancel.count");
    }

    public void markSubmit() {
        orderSubmitCounter.increment();
    }

    public void markCancel() {
        orderCancelCounter.increment();
    }
}
