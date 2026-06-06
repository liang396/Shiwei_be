package com.shiwei.seckill.seckill.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.seckill.mapper.SeckillOrderMapper;
import com.shiwei.seckill.seckill.model.entity.SeckillOrder;
import com.shiwei.seckill.seckill.service.SeckillStockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SeckillOrderCompensationJob {
    private static final long UNPAID_TIMEOUT_MINUTES = 30L;

    @Resource
    private SeckillOrderMapper seckillOrderMapper;
    @Resource
    private SeckillStockService seckillStockService;

    @Scheduled(fixedDelay = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(UNPAID_TIMEOUT_MINUTES);
        List<SeckillOrder> unpaidOrders = seckillOrderMapper.selectList(
            new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, 0)
                .lt(SeckillOrder::getCreatedAt, deadline)
                .last("limit 100")
        );
        for (SeckillOrder order : unpaidOrders) {
            seckillStockService.rollbackStock(order.getActivityId(), order.getSeckillGoodsId(), order.getUserId(), order.getBuyNum());
            order.setStatus(-1);
            seckillOrderMapper.updateById(order);
        }
    }
}

