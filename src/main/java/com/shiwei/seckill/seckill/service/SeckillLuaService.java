package com.shiwei.seckill.seckill.service;

import com.shiwei.seckill.seckill.model.dto.SeckillPreCheckDto;

public interface SeckillLuaService {
    Integer executePreCheck(SeckillPreCheckDto dto);
}

