package com.shiwei.seckill.pay.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.enums.OperatorTypeEnum;
import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.service.OrderService;
import com.shiwei.seckill.order.service.OrderStateMachineService;
import com.shiwei.seckill.order.service.support.OrderOperateContext;
import com.shiwei.seckill.pay.config.AlipayProperties;
import com.shiwei.seckill.pay.entity.PayLogEntity;
import com.shiwei.seckill.pay.mapper.PayLogMapper;
import com.shiwei.seckill.pay.service.PayService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AlipayPayServiceImpl implements PayService {
    @Resource
    private AlipayProperties alipayProperties;
    @Resource
    private OrderService orderService;
    @Resource
    private OrderStateMachineService orderStateMachineService;
    @Resource
    private PayLogMapper payLogMapper;

    @Override
    public String createPayPage(Long orderId) {
        OrderEntity order = orderService.getEntityById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatusEnum.PENDING_PAY.equals(OrderStatusEnum.fromCode(order.getOrderStatus()))) {
            throw new BizException("当前订单状态不允许发起模拟支付");
        }

        try {
            AlipayClient client = new DefaultAlipayClient(
                alipayProperties.getGateway(),
                alipayProperties.getAppId(),
                alipayProperties.getPrivateKey(),
                "json",
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType()
            );

            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            if (StringUtils.hasText(alipayProperties.getReturnUrl())) {
                request.setReturnUrl(alipayProperties.getReturnUrl());
            }
            if (StringUtils.hasText(alipayProperties.getNotifyUrl())) {
                request.setNotifyUrl(alipayProperties.getNotifyUrl());
            }

            String bizContent = "{"
                + "\"out_trade_no\":\"" + order.getOrderNo() + "\","
                + "\"total_amount\":\"" + order.getPayAmount() + "\","
                + "\"subject\":\"订单流转演示-" + order.getOrderNo() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
                + "}";
            request.setBizContent(bizContent);

            return client.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            throw new IllegalStateException("模拟支付页生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> payload) {
        String tradeStatus = payload.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }

        String orderNo = payload.get("out_trade_no");
        String payOrderNo = payload.get("trade_no");
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(payOrderNo)) {
            throw new BizException("模拟支付回调参数不完整");
        }

        OrderEntity order = orderService.getEntityByOrderNo(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }

        try {
            payLogMapper.insert(buildPayLog(order, payOrderNo, tradeStatus, payload));
        } catch (DuplicateKeyException ex) {
            return "success";
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
                return "success";
            }
            throw ex;
        }

        orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.PAY_SUCCESS,
            OrderOperateContext.builder()
                .operatorType(OperatorTypeEnum.PAY_SYSTEM)
                .operatorId(0L)
                .payChannel("MOCK_PAY")
                .remark("模拟支付回调成功")
                .operateTime(LocalDateTime.now())
                .build()
        );
        return "success";
    }

    private PayLogEntity buildPayLog(OrderEntity order, String payOrderNo, String tradeStatus, Map<String, String> payload) {
        PayLogEntity payLog = new PayLogEntity();
        payLog.setOrderId(order.getId());
        payLog.setOrderNo(order.getOrderNo());
        payLog.setPayOrderNo(payOrderNo);
        payLog.setPayChannel("MOCK_PAY");
        payLog.setTradeStatus(tradeStatus);
        payLog.setPayAmount(resolveAmount(payload.get("total_amount"), order.getPayAmount()));
        payLog.setNotifyPayload(payload.toString());
        payLog.setCreatedAt(LocalDateTime.now());
        return payLog;
    }

    private BigDecimal resolveAmount(String amountText, BigDecimal fallback) {
        if (!StringUtils.hasText(amountText)) {
            return fallback == null ? BigDecimal.ZERO : fallback;
        }
        return new BigDecimal(amountText);
    }
}
