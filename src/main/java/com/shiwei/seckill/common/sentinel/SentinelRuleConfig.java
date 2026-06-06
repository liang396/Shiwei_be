package com.shiwei.seckill.common.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelRuleConfig {
    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();
        rules.add(buildRule("order.submit", 200));
        rules.add(buildRule("pay.notify", 100));
        rules.add(buildRule("seckill.submit", 300));
        FlowRuleManager.loadRules(rules);
    }

    private FlowRule buildRule(String resource, double count) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(count);
        return rule;
    }
}

