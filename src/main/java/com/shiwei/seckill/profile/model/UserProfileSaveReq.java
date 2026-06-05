package com.shiwei.seckill.profile.model;

import lombok.Data;

@Data
public class UserProfileSaveReq {
    private Long userId;
    private String nickname;
    private String avatar;
}
