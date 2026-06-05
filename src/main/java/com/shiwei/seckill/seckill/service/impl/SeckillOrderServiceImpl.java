package com.shiwei.seckill.seckill.service.impl;

import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.seckill.config.SeckillConstants;
import com.shiwei.seckill.seckill.model.dto.SeckillGoodsSnapshot;
import com.shiwei.seckill.seckill.model.dto.SeckillMqMessage;
import com.shiwei.seckill.seckill.model.dto.SeckillPreCheckDto;
import com.shiwei.seckill.seckill.model.req.SeckillSubmitReq;
import com.shiwei.seckill.seckill.model.res.SeckillResultRes;
import com.shiwei.seckill.seckill.model.res.SeckillSubmitRes;
import com.shiwei.seckill.seckill.service.SeckillActivityService;
import com.shiwei.seckill.seckill.service.SeckillLuaService;
import com.shiwei.seckill.seckill.service.SeckillMqService;
import com.shiwei.seckill.seckill.service.SeckillOrderService;
import com.shiwei.seckill.seckill.service.SeckillStockService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;

@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {
    @Resource
    private SeckillActivityService seckillActivityService;
    @Resource
    private SeckillLuaService seckillLuaService;
    @Resource
    private SeckillMqService seckillMqService;
    @Resource
    private SeckillStockService seckillStockService;

    @Override
    public SeckillSubmitRes submitSeckill(Long userId, SeckillSubmitReq req) {
        SeckillGoodsSnapshot snapshot = seckillActivityService.getGoodsSnapshot(req.getActivityId(), req.getGoodsId());
        if (snapshot == null) {
            throw new BizException("秒杀商品不存在");
        }

        SeckillPreCheckDto dto = new SeckillPreCheckDto();
        dto.setActivityId(req.getActivityId());
        dto.setGoodsId(req.getGoodsId());
        dto.setUserId(userId);
        dto.setBuyNum(req.getBuyNum());

        Integer luaResult = seckillLuaService.executePreCheck(dto);
        if (!Integer.valueOf(SeckillConstants.LUA_SUCCESS).equals(luaResult)) {
            return fail(luaResult);
        }

        String messageId = UUID.randomUUID().toString();
        SeckillMqMessage message = new SeckillMqMessage();
        message.setMessageId(messageId);
        message.setActivityId(snapshot.getActivityId());
        message.setGoodsId(snapshot.getGoodsId());
        message.setUserId(userId);
        message.setBuyNum(req.getBuyNum());
        message.setSeckillPrice(snapshot.getSeckillPrice());
        message.setProductId(snapshot.getProductId());
        message.setProductItemId(snapshot.getProductItemId());
        message.setRequestTime(System.currentTimeMillis());
        message.setTraceId(messageId);
        seckillMqService.sendSeckillMessage(message);

        SeckillSubmitRes res = new SeckillSubmitRes();
        res.setSuccess(true);
        res.setMessage("抢购请求已受理");
        res.setResultStatus(0);
        res.setMessageId(messageId);
        return res;
    }

    @Override
    public SeckillResultRes queryResult(Long userId, Long activityId) {
        String result = seckillStockService.getResult(activityId, userId);
        SeckillResultRes res = new SeckillResultRes();
        if (result == null) {
            res.setResultStatus(-2);
            res.setMessage("未查询到结果");
            return res;
        }
        if ("0".equals(result)) {
            res.setResultStatus(0);
            res.setMessage("排队中");
            return res;
        }
        if ("-1".equals(result)) {
            res.setResultStatus(-1);
            res.setMessage("抢购失败");
            return res;
        }
        res.setResultStatus(1);
        res.setOrderId(Long.valueOf(result));
        res.setMessage("抢购成功");
        return res;
    }

    private SeckillSubmitRes fail(Integer code) {
        SeckillSubmitRes res = new SeckillSubmitRes();
        res.setSuccess(false);
        res.setResultStatus(1);
        switch (code) {
            case SeckillConstants.LUA_OUT_OF_STOCK:
                res.setMessage("库存不足");
                break;
            case SeckillConstants.LUA_ACTIVITY_NOT_FOUND:
                res.setMessage("活动不存在");
                break;
            case SeckillConstants.LUA_ACTIVITY_NOT_STARTED:
                res.setMessage("活动未开始");
                break;
            case SeckillConstants.LUA_ACTIVITY_ENDED:
                res.setMessage("活动已结束");
                break;
            case SeckillConstants.LUA_REPEAT_ORDER:
                res.setMessage("重复下单");
                break;
            default:
                res.setMessage("系统繁忙");
                break;
        }
        return res;
    }
}
