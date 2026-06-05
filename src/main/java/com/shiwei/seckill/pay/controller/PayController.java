package com.shiwei.seckill.pay.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.pay.model.req.AlipayCreateReq;
import com.shiwei.seckill.pay.service.PayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {
    @Resource
    private PayService payService;

    @PostMapping({"/mock/create", "/alipay/create"})
    public ApiResponse<?> create(@RequestBody AlipayCreateReq req) {
        return ApiResponse.success(payService.createPayPage(req.getOrderId()));
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
