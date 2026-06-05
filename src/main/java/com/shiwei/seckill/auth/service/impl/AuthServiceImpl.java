package com.shiwei.seckill.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.auth.mapper.AuthUserMapper;
import com.shiwei.seckill.auth.model.AuthUser;
import com.shiwei.seckill.auth.model.LoginReq;
import com.shiwei.seckill.auth.model.RegisterRequest;
import com.shiwei.seckill.auth.model.RegReq;
import com.shiwei.seckill.auth.model.SmsLoginRequest;
import com.shiwei.seckill.auth.model.UserAccount;
import com.shiwei.seckill.auth.service.AuthService;
import com.shiwei.seckill.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String DEFAULT_PASSWORD = "123456";

    private final AuthUserMapper authUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserAccount login(UserAccount req) {
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            throw new BizException("用户名和密码不能为空");
        }

        AuthUser user = authUserMapper.selectOne(
                new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getUsername, req.getUsername())
        );

        if (user == null && req.getUsername().matches("^1\\d{10}$")) {
            user = authUserMapper.selectOne(
                    new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getPhone, req.getUsername())
            );
        }

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }

        return toView(user);
    }

    @Override
    public UserAccount login(LoginReq req) {
        UserAccount loginReq = new UserAccount();
        loginReq.setUsername(req.getUserAccount());
        loginReq.setPassword(req.getPassword());
        return login(loginReq);
    }

    @Override
    public UserAccount register(RegisterRequest req) {
        if (!StringUtils.hasText(req.getPhone()) || !req.getPhone().matches("^1\\d{10}$")) {
            throw new BizException("手机号格式不正确");
        }
        if (!StringUtils.hasText(req.getPassword()) || req.getPassword().length() < 6) {
            throw new BizException("密码长度不能少于6位");
        }

        String username = StringUtils.hasText(req.getUsername()) ? req.getUsername().trim() : req.getPhone().trim();

        if (authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getPhone, req.getPhone().trim())) != null) {
            throw new BizException("该手机号已注册");
        }
        if (authUserMapper.selectOne(new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getUsername, username)) != null) {
            throw new BizException("该账号已存在");
        }

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone().trim());
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname().trim() : "拾味用户");
        user.setCreatedAt(LocalDateTime.now());
        authUserMapper.insert(user);
        return toView(user);
    }

    @Override
    public UserAccount register(RegReq req) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(StringUtils.hasText(req.getUserAccount()) ? req.getUserAccount().trim() : req.getVerifyKey().trim());
        registerRequest.setPassword(DEFAULT_PASSWORD);
        registerRequest.setPhone(req.getVerifyKey());
        registerRequest.setNickname(StringUtils.hasText(req.getUserAccount()) ? req.getUserAccount().trim() : req.getVerifyKey().trim());
        registerRequest.setCode(req.getVerifyCode());
        return register(registerRequest);
    }

    @Override
    public UserAccount smsLogin(SmsLoginRequest req) {
        if (!StringUtils.hasText(req.getPhone()) || !req.getPhone().matches("^1\\d{10}$")) {
            throw new BizException("手机号格式不正确");
        }
        AuthUser user = authUserMapper.selectOne(
                new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getPhone, req.getPhone().trim())
        );
        if (user == null) {
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setPhone(req.getPhone().trim());
            registerRequest.setUsername(req.getPhone().trim());
            registerRequest.setNickname(req.getPhone().trim());
            registerRequest.setPassword(DEFAULT_PASSWORD);
            user = authUserMapper.selectById(register(registerRequest).getUserId());
        }
        return toView(user);
    }

    @Override
    public UserAccount smsLogin(RegReq req) {
        SmsLoginRequest smsLoginRequest = new SmsLoginRequest();
        smsLoginRequest.setPhone(req.getVerifyKey());
        smsLoginRequest.setCode(req.getVerifyCode());
        return smsLogin(smsLoginRequest);
    }

    private UserAccount toView(AuthUser user) {
        UserAccount view = new UserAccount();
        view.setUserId(user.getUserId());
        view.setUsername(user.getUsername());
        view.setNickname(user.getNickname());
        view.setPhone(user.getPhone());
        return view;
    }
}
