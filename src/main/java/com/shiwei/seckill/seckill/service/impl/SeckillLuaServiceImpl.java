package com.shiwei.seckill.seckill.service.impl;

import com.shiwei.seckill.seckill.config.SeckillConstants;
import com.shiwei.seckill.seckill.config.SeckillRedisKey;
import com.shiwei.seckill.seckill.model.dto.SeckillPreCheckDto;
import com.shiwei.seckill.seckill.service.SeckillLuaService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class SeckillLuaServiceImpl implements SeckillLuaService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DefaultRedisScript<Long> seckillPreCheckScript;

    @Override
    public Integer executePreCheck(SeckillPreCheckDto dto) {
        if (stringRedisTemplate == null || seckillPreCheckScript == null) {
            return SeckillConstants.LUA_SYSTEM_BUSY;
        }
        Long result = stringRedisTemplate.execute(
                seckillPreCheckScript,
                Arrays.asList(
                        SeckillRedisKey.activity(dto.getActivityId()),
                        SeckillRedisKey.stock(dto.getActivityId(), dto.getGoodsId()),
                        SeckillRedisKey.userOrder(dto.getActivityId(), dto.getUserId()),
                        SeckillRedisKey.result(dto.getActivityId(), dto.getUserId())
                ),
                String.valueOf(dto.getActivityId()),
                String.valueOf(dto.getGoodsId()),
                String.valueOf(dto.getUserId()),
                String.valueOf(dto.getBuyNum())
        );
        return result == null ? SeckillConstants.LUA_SYSTEM_BUSY : result.intValue();
    }

}
