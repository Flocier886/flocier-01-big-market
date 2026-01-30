package com.flocier.domain.strategy.service.rule.tree.factory.engine;

import com.flocier.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

import java.util.Date;

public interface IDecisionTreeEngine {

    DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId, Date endDateTime);
}
