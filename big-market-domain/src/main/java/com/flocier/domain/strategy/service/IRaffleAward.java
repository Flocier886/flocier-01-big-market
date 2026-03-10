package com.flocier.domain.strategy.service;

import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.model.vo.StrategyAwardStockKeyVO;

import java.util.List;

public interface IRaffleAward {
    /**
     * 根据策略ID查询抽奖奖品列表配置
     *
     * @param strategyId 策略ID
     * @return 奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardList(Long strategyId);

    /**
     * 根据策略ID查询抽奖奖品列表配置
     *
     * @param activityId 策略ID
     * @return 奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardListByActivityId(Long activityId);

    /**
     * 查询有效活动的奖品配置
     *
     * @return 奖品配置列表
     */
    List<StrategyAwardStockKeyVO> queryOpenActivityStrategyAwardList();
}
