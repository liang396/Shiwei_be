package com.shiwei.seckill.address.model;

import lombok.Data;

@Data
public class AddressRecord {
    private Long addressId;
    private String consignee;
    private String mobile;
    private String address;
    private String mobileRaw;
    private String addressRaw;
    private Boolean isDefault;
}

