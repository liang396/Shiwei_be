package com.shiwei.seckill.auth.service;

import com.shiwei.seckill.auth.model.RegisterRequest;
import com.shiwei.seckill.auth.model.LoginReq;
import com.shiwei.seckill.auth.model.RegReq;
import com.shiwei.seckill.auth.model.SmsLoginRequest;
import com.shiwei.seckill.auth.model.UserAccount;

public interface AuthService {
    UserAccount login(UserAccount req);

    UserAccount login(LoginReq req);

    UserAccount register(RegisterRequest req);

    UserAccount register(RegReq req);

    UserAccount smsLogin(SmsLoginRequest req);

    UserAccount smsLogin(RegReq req);
}
