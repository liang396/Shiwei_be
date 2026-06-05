package com.shiwei.seckill.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_pay_log")
public class PayLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private String payOrderNo;
    private String payChannel;
    private String tradeStatus;
    private BigDecimal payAmount;
    private String notifyPayload;
    private LocalDateTime createdAt;
}
