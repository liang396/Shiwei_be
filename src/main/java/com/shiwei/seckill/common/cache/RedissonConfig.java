package com.shiwei.seckill.common.cache;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.redis.host:127.0.0.1}") String host,
                                         @Value("${spring.redis.port:6379}") int port,
                                         @Value("${spring.redis.password:}") String password) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }
}
