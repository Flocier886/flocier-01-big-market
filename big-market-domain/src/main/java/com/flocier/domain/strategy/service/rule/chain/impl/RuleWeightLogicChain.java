package com.flocier.domain.strategy.service.rule.chain.impl;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component("rule_weight")
public class RuleWeightLogicChain extends AbstractLogicChain {
    @Resource
    private IStrategyRepository repository;
    @Resource
    protected IStrategyDisPatch strategyDisPatch;

    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-权重开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        //查询对应规则的相关数据
        String ruleValue=repository.queryStrategyRuleValue(strategyId,ruleModel());
        Map<Long,String> analyticalValueGroup=getAnalyticalValue(ruleValue);
        if(analyticalValueGroup==null || analyticalValueGroup.isEmpty())return null;
        //选择对应区间的ruleWeightValueKey值
        List<Long> analyticalSortedKeys=new ArrayList<>(analyticalValueGroup.keySet());
        //已修好此处的排序bug
        analyticalSortedKeys.sort(Comparator.reverseOrder());
        //查询用户抽奖权重
        Integer userScore = repository.queryActivityAccountTotalUseCount(userId, strategyId);
        Long weightValue=analyticalSortedKeys.stream()
                .filter(key->userScore>=key)
                .findFirst()
                .orElse(null);
        //判断好了权重过后就开始对对应权重表进行抽奖
        if(weightValue!=null){
            Integer awardId=strategyDisPatch.getRandomAwardId(strategyId,analyticalValueGroup.get(weightValue));
            log.info("抽奖责任链-权重接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
            return DefaultChainFactory.StrategyAwardVO
                    .builder()
                    .awardId(awardId)
                    .logicModel(ruleModel())
                    .build();
        }
        // 过滤其他责任链
        log.info("抽奖责任链-权重放行 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        return this.next().logic(userId,strategyId);
    }
    @Override
    protected String ruleModel() {
        return DefaultChainFactory.LogicModel.RULE_WEIGHT.getCode();
    }
    private Map<Long, String> getAnalyticalValue(String ruleValue) {
        String[] ruleValueGroups = ruleValue.split(Constants.SPACE);
        Map<Long, String> ruleValueMap = new HashMap<>();
        for (String ruleValueKey : ruleValueGroups) {
            // 检查输入是否为空
            if (ruleValueKey == null || ruleValueKey.isEmpty()) {
                return ruleValueMap;
            }
            // 分割字符串以获取键和值
            String[] parts = ruleValueKey.split(Constants.COLON);
            if (parts.length != 2) {
                throw new IllegalArgumentException("rule_weight rule_rule invalid input format" + ruleValueKey);
            }
            ruleValueMap.put(Long.parseLong(parts[0]), ruleValueKey);
        }
        return ruleValueMap;
    }
}
