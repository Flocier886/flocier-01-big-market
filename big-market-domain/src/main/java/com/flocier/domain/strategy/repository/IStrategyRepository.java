package com.flocier.domain.strategy.repository;

import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;

import java.util.List;
import java.util.Map;

public interface IStrategyRepository {
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    void storeStrategyAwardSearchRateTable(Long strategyId, Integer rateRange, Map<Integer, Integer> shuffleStrategyAwardSearchRateTable);

    int getRateRange(Long strategyId);

    Integer getStrategyAwardAssemble(Long strategyId, Integer rateKey);
}
