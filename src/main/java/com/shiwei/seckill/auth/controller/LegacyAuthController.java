package com.shiwei.seckill.auth.controller;

import com.shiwei.seckill.auth.model.LoginReq;
import com.shiwei.seckill.auth.model.RegReq;
import com.shiwei.seckill.auth.model.SmsCodeRequest;
import com.shiwei.seckill.auth.model.UserAccount;
import com.shiwei.seckill.auth.service.AuthService;
import com.shiwei.seckill.auth.service.SmsCodeService;
import com.shiwei.seckill.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LegacyAuthController {
    private final AuthService authService;
    private final SmsCodeService smsCodeService;

    @PostMapping("/front/account/login/login")
    public ApiResponse<UserAccount> login(@ModelAttribute LoginReq req) {
        if (req.getVerifyKey() != null && !req.getVerifyKey().trim().isEmpty()) {
            if (!smsCodeService.verifyCode(req.getVerifyKey(), req.getVerifyCode())) {
                return ApiResponse.fail("验证码错误或已过期");
            }
        }
        return ApiResponse.successMessage("login success", authService.login(req));
    }

    @PostMapping("/front/account/login/register")
    public ApiResponse<UserAccount> register(@ModelAttribute RegReq req) {
        if (!smsCodeService.verifyCode(req.getVerifyKey(), req.getVerifyCode())) {
            return ApiResponse.fail("验证码错误或已过期");
        }
        return ApiResponse.successMessage("register success", authService.register(req));
    }

    @PostMapping("/front/account/login/doSmsLogin")
    public ApiResponse<UserAccount> smsLogin(@ModelAttribute RegReq req) {
        if (!smsCodeService.verifyCode(req.getVerifyKey(), req.getVerifyCode())) {
            return ApiResponse.fail("验证码错误或已过期");
        }
        return ApiResponse.successMessage("login success", authService.smsLogin(req));
    }

    @PostMapping("/front/sys/captcha/mobile")
    public ApiResponse<String> sendSmsCode(@ModelAttribute SmsCodeRequest req) {
        String code = smsCodeService.sendCode(req.getPhone());
        return ApiResponse.successMessage("验证码已生成（当前为开发模式）", code);
    }
}
