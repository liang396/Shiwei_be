package com.shiwei.seckill.pay.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.service.OrderService;
import com.shiwei.seckill.pay.model.req.AlipayCreateReq;
import com.shiwei.seckill.pay.service.PayService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
public class PayController {
    @Resource
    private PayService payService;
    @Resource
    private OrderService orderService;

    @PostMapping({"/mock/create", "/alipay/create"})
    public ApiResponse<?> create(@RequestBody AlipayCreateReq req) {
        Long orderId = null;
        if (req.getOrderNo() != null && !req.getOrderNo().trim().isEmpty()) {
            OrderEntity order = orderService.getEntityByOrderNo(req.getOrderNo().trim());
            if (order != null) {
                orderId = order.getId();
            }
        } else if (req.getOrderId() != null) {
            orderId = req.getOrderId();
        }

        if (orderId == null) {
            throw new BizException("订单不存在");
        }
        return ApiResponse.success(payService.createPayPage(orderId));
    }

    @PostMapping({"/mock/notify", "/alipay/notify"})
    public String notifyResult(HttpServletRequest request) {
        Map<String, String> payload = new HashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            payload.put(entry.getKey(), values != null && values.length > 0 ? values[0] : null);
        }
        return payService.handleNotify(payload);
    }
}
