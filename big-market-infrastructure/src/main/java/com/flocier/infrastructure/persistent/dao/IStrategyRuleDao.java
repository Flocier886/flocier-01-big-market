package com.flocier.infrastructure.persistent.dao;

import com.flocier.infrastructure.persistent.po.StrategyRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IStrategyRuleDao {
    StrategyRule queryStrategyRuleByStrategyIdWithRuleModel(Long strategyId, String ruleModel);
}
