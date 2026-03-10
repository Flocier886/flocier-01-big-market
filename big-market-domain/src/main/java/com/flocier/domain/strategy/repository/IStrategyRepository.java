package com.flocier.domain.strategy.repository;

import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.entity.StrategyRuleEntity;
import com.flocier.domain.strategy.model.vo.RuleTreeVO;
import com.flocier.domain.strategy.model.vo.RuleWeightVO;
import com.flocier.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.flocier.domain.strategy.model.vo.StrategyAwardStockKeyVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IStrategyRepository {
    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Integer> shuffleStrategyAwardSearchRateTable);

    int getRateRange(Long strategyId);

    int getRateRange(String key);

    Integer getStrategyAwardAssemble(String key, Integer rateKey);

    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    StrategyRuleEntity queryStrategyRuleEntity(Long strategyId, String ruleModel);

    String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel);

    StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId);

    String queryStrategyRuleValue(Long strategyId, String ruleModel);

    RuleTreeVO queryRuleTreeVOByTreeId(String ruleModels);

    void cacheStrategyAwardCount(String cacheKey, Integer awardCount);

    void awardStockConsumeSendQueue(StrategyAwardStockKeyVO build);

    StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException;

    StrategyAwardStockKeyVO takeQueueValue(Long strategyId, Integer awardId) throws InterruptedException;

    void updateStrategyAwardStock(Long strategyId, Integer awardId);

    StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId);

    Long queryStrategyIdByActivityId(Long activityId);

    Integer queryTodayUserRaffleCount(String userId, Long strategyId);

    Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);

    Boolean subtractionAwardStock(String cacheKey);

    Boolean subtractionAwardStock(String cacheKey, Date endDateTime);

    Integer queryActivityAccountTotalUseCount(String userId, Long strategyId);

    List<RuleWeightVO> queryAwardRuleWeight(Long strategyId);

    String queryStrategyAwardValue(Integer awardId);

    List<StrategyAwardStockKeyVO> queryOpenActivityStrategyAwardList();

}
