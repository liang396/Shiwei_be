package com.shiwei.seckill.profile.service;

import com.shiwei.seckill.auth.model.UserAccount;
import com.shiwei.seckill.profile.model.UserProfileSaveReq;

public interface UserProfileService {
    UserAccount currentProfile();

    UserAccount save(UserProfileSaveReq req);
}
