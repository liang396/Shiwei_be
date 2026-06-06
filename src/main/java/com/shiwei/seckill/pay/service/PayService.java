package com.shiwei.seckill.pay.service;

import java.util.Map;

public interface PayService {
    String createPayPage(Long orderId);

    String handleNotify(Map<String, String> payload);
}

