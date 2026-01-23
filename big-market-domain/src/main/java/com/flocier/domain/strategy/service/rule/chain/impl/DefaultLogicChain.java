package com.flocier.domain.strategy.service.rule.chain.impl;

import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.chain.AbstractLogicChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("default")
public class DefaultLogicChain extends AbstractLogicChain {
    @Resource
    protected IStrategyDisPatch strategyDisPatch;
    @Override
    public Integer logic(String userId, Long strategyId) {
        //执行默认的抽奖流程
        Integer awardId=strategyDisPatch.getRandomAwardId(strategyId);
        log.info("抽奖责任链-默认处理 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
        return awardId;
    }

    @Override
    protected String ruleModel() {
        return "default";
    }
}
