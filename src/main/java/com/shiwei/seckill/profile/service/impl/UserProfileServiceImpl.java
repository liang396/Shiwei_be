package com.shiwei.seckill.profile.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.auth.model.UserAccount;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.profile.model.UserProfileSaveReq;
import com.shiwei.seckill.profile.service.UserProfileService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UserProfileServiceImpl implements UserProfileService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storagePath = Paths.get("data", "user-profile.json");
    private UserAccount profile;

    @PostConstruct
    public void init() {
        load();
        if (profile == null) {
            profile = defaultProfile();
            persist();
        }
    }

    @Override
    public synchronized UserAccount currentProfile() {
        return copy(profile);
    }

    @Override
    public synchronized UserAccount save(UserProfileSaveReq req) {
        if (req.getNickname() == null || req.getNickname().trim().isEmpty()) {
            throw new BizException("请填写昵称");
        }
        profile.setNickname(req.getNickname().trim());
        profile.setAvatar((req.getAvatar() == null || req.getAvatar().trim().isEmpty()) ? "🙂" : req.getAvatar().trim());
        persist();
        return copy(profile);
    }

    private void load() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            profile = objectMapper.readValue(storagePath.toFile(), UserAccount.class);
        } catch (IOException e) {
            throw new BizException("用户资料加载失败");
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), profile);
        } catch (IOException e) {
            throw new BizException("用户资料保存失败");
        }
    }

    private UserAccount defaultProfile() {
        UserAccount user = new UserAccount();
        user.setUserId(1L);
        user.setUsername("demo");
        user.setNickname("拾味用户");
        user.setPhone("13812345678");
        user.setAvatar("🙂");
        return user;
    }

    private UserAccount copy(UserAccount source) {
        UserAccount user = new UserAccount();
        user.setUserId(source.getUserId());
        user.setUsername(source.getUsername());
        user.setNickname(source.getNickname());
        user.setPhone(source.getPhone());
        user.setAvatar(source.getAvatar());
        return user;
    }
}
