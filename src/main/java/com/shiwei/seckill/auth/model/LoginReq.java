package com.shiwei.seckill.auth.model;

import lombok.Data;

@Data
public class LoginReq {
    private String userAccount;
    private String password;
    private String verifyCode;
    private String verifyKey;
    private Boolean encrypt = false;
}

