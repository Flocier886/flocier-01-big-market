package com.flocier.domain.strategy.service.rule.tree.factory;

import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.model.vo.RuleTreeVO;
import com.flocier.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.flocier.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.flocier.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultTreeFactory {
    private final Map<String, ILogicTreeNode> logicTreeNodeMap;

    public DefaultTreeFactory(Map<String, ILogicTreeNode> logicTreeNodeMap) {
        this.logicTreeNodeMap = logicTreeNodeMap;
    }
    public IDecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO){
        return new DecisionTreeEngine(logicTreeNodeMap,ruleTreeVO);
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TreeActionEntity {
        private RuleLogicCheckTypeVO ruleLogicCheckType;
        private StrategyAwardData strategyAwardData;
    }
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StrategyAwardData {
        /** 抽奖奖品ID - 内部流转使用 */
        private Integer awardId;
        /** 抽奖奖品规则 */
        private String awardRuleValue;
    }

}
