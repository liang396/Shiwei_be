package com.shiwei.seckill.auth.model;

import lombok.Data;

@Data
public class SmsLoginRequest {
    private String phone;
    private String code;
}
