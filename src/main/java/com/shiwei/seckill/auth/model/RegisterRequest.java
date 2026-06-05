package com.shiwei.seckill.auth.model;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String code;
}
