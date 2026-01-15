package com.flocier.infrastructure.persistent.repository;

import cn.hutool.core.bean.BeanUtil;
import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.infrastructure.persistent.dao.IStrategyAwardDao;
import com.flocier.infrastructure.persistent.po.StrategyAward;
import com.flocier.infrastructure.persistent.redis.IRedisService;
import com.flocier.types.common.Constants;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class StrategyRepository implements IStrategyRepository {

    @Resource
    private IRedisService redisService;
    @Resource
    private IStrategyAwardDao strategyAwardDao;
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        //先从redis中查找获取
        String cacheKey= Constants.RedisKey.STRATEGY_AWARD_KEY+strategyId;
        List<StrategyAwardEntity>strategyAwardEntities=redisService.getValue(cacheKey);
        if(strategyAwardEntities!=null && !strategyAwardEntities.isEmpty())return strategyAwardEntities;
        //没有就查找数据库
        List<StrategyAward>strategyAwards=strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        strategyAwardEntities= BeanUtil.copyToList(strategyAwards, StrategyAwardEntity.class);
        //再添加进redis
        redisService.setValue(cacheKey,strategyAwardEntities);
        return strategyAwardEntities;
    }

    @Override
    public void storeStrategyAwardSearchRateTable(Long strategyId, Integer rateRange, Map<Integer, Integer> shuffleStrategyAwardSearchRateTable) {
        //存储对应策略表的策略范围值
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId,rateRange);
        //存储对应策略表的概率查找表信息
        Map<Integer,Integer>cacheRateTable=redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId);
        cacheRateTable.putAll(shuffleStrategyAwardSearchRateTable);
    }

    @Override
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId);
    }

    @Override
    public Integer getStrategyAwardAssemble(Long strategyId, Integer rateKey) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId,rateKey);
    }
}
