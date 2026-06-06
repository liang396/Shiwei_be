package com.shiwei.seckill.profile.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UserProfileSaveReq {
    private Long userId;
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    private String avatar;
}

