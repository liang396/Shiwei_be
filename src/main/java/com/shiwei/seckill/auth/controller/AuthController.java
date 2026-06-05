package com.shiwei.seckill.auth.controller;

import com.shiwei.seckill.auth.model.RegisterRequest;
import com.shiwei.seckill.auth.model.SmsCodeRequest;
import com.shiwei.seckill.auth.model.SmsLoginRequest;
import com.shiwei.seckill.auth.model.UserAccount;
import com.shiwei.seckill.auth.service.AuthService;
import com.shiwei.seckill.auth.service.SmsCodeService;
import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.common.security.RequestRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SmsCodeService smsCodeService;
    private final RequestRateLimitService requestRateLimitService;

    @PostMapping("/login")
    public ApiResponse<UserAccount> login(@RequestBody UserAccount req) {
        return ApiResponse.successMessage("login success", authService.login(req));
    }

    @PostMapping("/sms-code")
    public ApiResponse<String> sendSmsCode(@Valid @RequestBody SmsCodeRequest req) {
        requestRateLimitService.guard("rate:sms:" + req.getPhone(), 5);
        String code = smsCodeService.sendCode(req.getPhone());
        return ApiResponse.successMessage("验证码已生成（当前为开发模式）", code);
    }

    @PostMapping("/register")
    public ApiResponse<UserAccount> register(@Valid @RequestBody RegisterRequest req) {
        if (!smsCodeService.verifyCode(req.getPhone(), req.getCode())) {
            return ApiResponse.fail("验证码错误或已过期");
        }
        return ApiResponse.successMessage("register success", authService.register(req));
    }

    @PostMapping("/sms-login")
    public ApiResponse<UserAccount> smsLogin(@Valid @RequestBody SmsLoginRequest req) {
        if (!smsCodeService.verifyCode(req.getPhone(), req.getCode())) {
            return ApiResponse.fail("验证码错误或已过期");
        }
        return ApiResponse.successMessage("login success", authService.smsLogin(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserAccount> me() {
        UserAccount user = new UserAccount();
        user.setUserId(1L);
        user.setUsername("demo");
        user.setNickname("demo-user");
        return ApiResponse.success(user);
    }
}
