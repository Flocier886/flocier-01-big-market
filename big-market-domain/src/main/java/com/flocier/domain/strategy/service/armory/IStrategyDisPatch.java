package com.flocier.domain.strategy.service.armory;

public interface IStrategyDisPatch {
    Integer getRandomAwardId(Long strategyId);
    Integer getRandomAwardId(Long strategyId,String ruleWeightValue);
    void cacheStrategyAwardCount(Long startegyId,Integer awardId,Integer awardCount);
    Boolean subtractionAwardStock(Long strategyId, Integer awardId);
}
