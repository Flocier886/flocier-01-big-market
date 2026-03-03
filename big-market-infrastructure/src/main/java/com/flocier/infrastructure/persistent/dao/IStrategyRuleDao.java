package com.flocier.infrastructure.persistent.dao;

import com.flocier.infrastructure.persistent.po.StrategyRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IStrategyRuleDao {
    StrategyRule queryStrategyRuleByStrategyIdWithRuleModel(@Param("strategyId")Long strategyId,@Param("ruleModel") String ruleModel);

    String queryStrategyRuleValue(StrategyRule strategyRule);
}
