package com.shiwei.seckill.seckill.service;

import com.shiwei.seckill.seckill.config.SeckillRedisKey;
import com.shiwei.seckill.seckill.service.impl.SeckillStockServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillStockServiceTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private SeckillStockServiceImpl seckillStockService;

    @Test
    void shouldRollbackRedisStockAndUserOrderFlag() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);

        seckillStockService.rollbackStock(1L, 2L, 3L, 2);

        verify(setOperations).remove(SeckillRedisKey.userOrder(1L, 3L), String.valueOf(3L));
        verify(valueOperations).increment(SeckillRedisKey.stock(1L, 2L), 2L);
    }
}
