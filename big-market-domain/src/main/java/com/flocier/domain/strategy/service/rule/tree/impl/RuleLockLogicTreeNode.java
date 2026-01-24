package com.flocier.domain.strategy.service.rule.tree.impl;

import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import com.flocier.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.flocier.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("rule_lock")
public class RuleLockLogicTreeNode implements ILogicTreeNode {
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        //暂时先不设计任何流程，只放行用于该阶段测试
        return DefaultTreeFactory.TreeActionEntity
                .builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
}
