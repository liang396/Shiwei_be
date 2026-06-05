package com.shiwei.seckill.auth.model;

import lombok.Data;

@Data
public class RegReq {
    private String userAccount;
    private String password;
    private String verifyCode;
    private String verifyKey;
    private Boolean encrypt = false;
    private Integer bindType;
    private Integer activityId;
    private Integer sourceUserId;
    private String sourceUccCode;
}
