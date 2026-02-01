package com.flocier.domain.strategy.service.rule.chain.impl;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

@Slf4j
@Component("rule_blacklist")
public class BlackListLogicChain extends AbstractLogicChain {
    @Resource
    private IStrategyRepository repository;
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-黑名单开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        //查找对应策略的黑名单规则的数据
        String ruleValue=repository.queryStrategyRuleValue(strategyId,ruleModel());
        String[] splitRuleValue=ruleValue.split(Constants.COLON);
        Integer awardId=Integer.parseInt(splitRuleValue[0]);
        //判断用户id是否处于黑名单
        String[] userBlackIds=splitRuleValue[1].split(Constants.SPLIT);
        if(Arrays.asList(userBlackIds).contains(userId)){
            log.info("抽奖责任链-黑名单接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
            return DefaultChainFactory.StrategyAwardVO
                    .builder()
                    .awardId(awardId)
                    .logicModel(ruleModel())
                    //TODO这里写死了，后续应该是查询数据库配置
                    .awardRuleValue("0.01,1")
                    .build();
        }
        //不处于黑名单就继续过滤其他责任链
        log.info("抽奖责任链-黑名单放行 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        return this.next().logic(userId,strategyId);
    }

    @Override
    protected String ruleModel() {
        return DefaultChainFactory.LogicModel.RULE_BLACKLIST.getCode();
    }

}
