package com.flocier.domain.strategy.service.rule.impl;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RuleMatterEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.annotation.LogicStrategy;
import com.flocier.domain.strategy.service.rule.ILogicFilter;
import com.flocier.domain.strategy.service.rule.factory.DefaultLogicFactory;
import com.flocier.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Component
@LogicStrategy(logicMode=DefaultLogicFactory.LogicModel.RULE_WEIGHT)
public class RuleWeightLogicFilter implements ILogicFilter<RaffleActionEntity.BeforeRaffleEntity> {
    @Resource
    private IStrategyRepository repository;
    //此处暂时设置为常量，后续会优化
    public Long userScore=4500L;
    @Override
    public RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-权重范围 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        //查询规则配置
        String ruleValue=repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getAwardId(),ruleMatterEntity.getRuleModel());

        Map<Long,String> analyticalValueGroup=getAnalyticalValue(ruleValue);
        if(analyticalValueGroup==null || analyticalValueGroup.isEmpty()){
            return RaffleActionEntity.<RaffleActionEntity.BeforeRaffleEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }
        //选择对应区间的ruleWeightValueKey值
        List<Long> analyticalSortedKeys=new ArrayList<>(analyticalValueGroup.keySet());
        //以修好此处的排序bug
        analyticalSortedKeys.sort(Comparator.reverseOrder());
        Long weightValue=analyticalSortedKeys.stream()
                .filter(key->userScore>=key)
                .findFirst()
                .orElse(null);
        //log.info("用户积分: {},当前权重: {}",userScore,weightValue);
        if(weightValue!=null){
            return RaffleActionEntity.<RaffleActionEntity.BeforeRaffleEntity>builder()
                    .ruleModel(DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode())
                    .data(RaffleActionEntity.BeforeRaffleEntity.builder()
                            .strategyId(ruleMatterEntity.getStrategyId())
                            .ruleWeightValueKey(analyticalValueGroup.get(weightValue))
                            .build())
                    .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                    .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                    .build();
        }
        return RaffleActionEntity.<RaffleActionEntity.BeforeRaffleEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
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
