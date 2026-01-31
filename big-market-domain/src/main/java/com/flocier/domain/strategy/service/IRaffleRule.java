package com.flocier.domain.strategy.service;

import com.flocier.domain.strategy.model.vo.RuleWeightVO;

import java.util.List;
import java.util.Map;

public interface IRaffleRule {
    Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);

    List<RuleWeightVO> queryAwardRuleWeight(Long strategyId);

    List<RuleWeightVO> queryAwardRuleWeightByActivityId(Long activityId);
}
