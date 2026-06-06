package com.shiwei.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class OrderEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer orderStatus;
    private BigDecimal goodsAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String payChannel;
    private Integer sourceType;
    private Long addressId;
    private String consignee;
    private String mobile;
    private String fullAddress;
    private Long couponId;
    private String couponTitle;
    private LocalDateTime payTime;
    private LocalDateTime canceledTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

