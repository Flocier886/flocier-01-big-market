package com.flocier.domain.strategy.service.rule.filter.impl;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RuleMatterEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.annotation.LogicStrategy;
import com.flocier.domain.strategy.service.rule.filter.ILogicFilter;
import com.flocier.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import com.flocier.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_BLACKLIST)
public class RuleBlackListLogicFilter implements ILogicFilter<RaffleActionEntity.BeforeRaffleEntity> {
    @Resource
    private IStrategyRepository repository;


    @Override
    public RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤:黑名单 userId:{} strategyId:{} ruleModel:{}",ruleMatterEntity.getUserId(),ruleMatterEntity.getStrategyId(),ruleMatterEntity.getRuleModel());
        String userId=ruleMatterEntity.getUserId();

        //查询规则的配置
        String ruleValue=repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getAwardId(),ruleMatterEntity.getRuleModel());
        String[] splitRuleValue=ruleValue.split(Constants.COLON);
        Integer awardId=Integer.parseInt(splitRuleValue[0]);
        //判断是否是黑名单，是的话拦截并返回对象
        String[] userBlackIds=splitRuleValue[1].split(Constants.SPLIT);
        if(Arrays.asList(userBlackIds).contains(userId)){
            return RaffleActionEntity.<RaffleActionEntity.BeforeRaffleEntity>builder()
                    .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
                    .data(RaffleActionEntity.BeforeRaffleEntity.builder()
                            .strategyId(ruleMatterEntity.getStrategyId())
                            .awardId(awardId)
                            .build())
                    .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                    .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                    .build();
        }
        //不处于黑名单，放行让下一个规则过滤
        return RaffleActionEntity.<RaffleActionEntity.BeforeRaffleEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }
}
