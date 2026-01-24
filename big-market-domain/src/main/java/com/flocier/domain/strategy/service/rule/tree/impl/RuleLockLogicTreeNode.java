package com.flocier.domain.strategy.service.rule.tree.impl;

import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.flocier.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component("rule_lock")
public class RuleLockLogicTreeNode implements ILogicTreeNode {
    @Resource
    private IStrategyRepository repository;

    private Long userRaffleCount=0L;
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        /**
        //查询规则配置
        String ruleValue=repository.queryStrategyRuleValue(strategyId,awardId,"rule_lock");
        Long ruleRaffleCount=Long.parseLong(ruleValue);
        //判断是否需要拦截
        if(userRaffleCount>=ruleRaffleCount){
            DefaultTreeFactory.TreeActionEntity
                    .builder()
                    .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                    .build();
        }**/
        //返回结果
        return DefaultTreeFactory.TreeActionEntity
                .builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
}
