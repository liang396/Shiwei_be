package com.shiwei.seckill.order.model;

import lombok.Data;

@Data
public class OrderDetailUpdateReq {
    private Long addressId;
    private String consignee;
    private String mobile;
    private String address;
    private Long couponId;
    private String couponTitle;
    private String payChannel;
}
