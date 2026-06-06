package com.shiwei.seckill.common.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.shiwei.seckill.common.exception.SentinelBlockedException;
import org.springframework.stereotype.Component;

@Component
public class SentinelSupport {
    public Entry enter(String resource) {
        try {
            return SphU.entry(resource);
        } catch (BlockException e) {
            throw new SentinelBlockedException("当前访问量较大，请稍后重试");
        }
    }
}

