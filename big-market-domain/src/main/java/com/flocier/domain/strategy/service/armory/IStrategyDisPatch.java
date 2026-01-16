package com.flocier.domain.strategy.service.armory;

public interface IStrategyDisPatch {
    Integer getRandomAwardId(Long strategyId);
    Integer getRandomAwardId(Long strategyId,String ruleWeightValue);
}
