package com.shiwei.seckill.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AesSecurityUtil {
    private final SecretKeySpec keySpec;

    public AesSecurityUtil(@Value("${shiwei.security.aes-key:ShiWeiDemoKey123}") String aesKey) {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[16];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, normalized.length));
        this.keySpec = new SecretKeySpec(normalized, "AES");
    }

    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return plaintext;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("敏感信息加密失败", e);
        }
    }

    public String decryptOrRaw(String ciphertext) {
        if (!StringUtils.hasText(ciphertext)) {
            return ciphertext;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return ciphertext;
        }
    }
}

