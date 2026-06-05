package com.shiwei.seckill.order.service;

public interface OrderDuplicateGuardService {
    void guardSubmit(Long userId);

    void guardCancel(String orderNo);
}
