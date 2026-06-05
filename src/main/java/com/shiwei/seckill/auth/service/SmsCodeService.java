package com.shiwei.seckill.auth.service;

public interface SmsCodeService {
    String sendCode(String phone);

    boolean verifyCode(String phone, String code);
}
