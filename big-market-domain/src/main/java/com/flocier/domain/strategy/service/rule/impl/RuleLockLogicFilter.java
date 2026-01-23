package com.flocier.domain.strategy.service.rule.impl;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RuleMatterEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.annotation.LogicStrategy;
import com.flocier.domain.strategy.service.rule.ILogicFilter;
import com.flocier.domain.strategy.service.rule.factory.DefaultLogicFactory;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_LOCK)
public class RuleLockLogicFilter implements ILogicFilter<RaffleActionEntity.CenterRaffleEntity> {
    @Resource
    private IStrategyRepository repository;

    private Long userRaffleCount=0L;
    @Override
    public RaffleActionEntity<RaffleActionEntity.CenterRaffleEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-次数锁 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        //查询规则的配置
        String ruleValue=repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getAwardId(),ruleMatterEntity.getRuleModel());
        Long ruleRaffleCount=Long.parseLong(ruleValue);
        //判断是否需要拦截
        if(userRaffleCount>=ruleRaffleCount){
            return RaffleActionEntity.<RaffleActionEntity.CenterRaffleEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }
        return RaffleActionEntity.<RaffleActionEntity.CenterRaffleEntity>builder()
                .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                .build();
    }
}
