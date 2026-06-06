package com.shiwei.seckill.address.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class AddressSaveReq {
    private Long addressId;
    @NotBlank(message = "收货人不能为空")
    private String consignee;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;
    @NotBlank(message = "地址不能为空")
    private String address;
    private Boolean isDefault;
}

