package com.flocier.infrastructure.persistent.repository;

import cn.hutool.core.bean.BeanUtil;
import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.entity.StrategyRuleEntity;
import com.flocier.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.infrastructure.persistent.dao.IStrategyAwardDao;
import com.flocier.infrastructure.persistent.dao.IStrategyDao;
import com.flocier.infrastructure.persistent.dao.IStrategyRuleDao;
import com.flocier.infrastructure.persistent.po.Strategy;
import com.flocier.infrastructure.persistent.po.StrategyAward;
import com.flocier.infrastructure.persistent.po.StrategyRule;
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
    @Resource
    private IStrategyDao strategyDao;
    @Resource
    private IStrategyRuleDao strategyRuleDao;
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
    public void storeStrategyAwardSearchRateTable(String key, Integer rateRange, Map<Integer, Integer> shuffleStrategyAwardSearchRateTable) {
        //存储对应策略表的策略范围值
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key,rateRange);
        //存储对应策略表的概率查找表信息
        Map<Integer,Integer>cacheRateTable=redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+key);
        cacheRateTable.putAll(shuffleStrategyAwardSearchRateTable);
    }

    @Override
    public int getRateRange(Long strategyId) {
        return getRateRange(String.valueOf(strategyId));
    }
    @Override
    public int getRateRange(String key) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key);
    }

    @Override
    public Integer getStrategyAwardAssemble(String key, Integer rateKey) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+key,rateKey);
    }

    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        //先从redis中查找获取
        String cacheKey= Constants.RedisKey.STRATEGY_KEY+strategyId;
        StrategyEntity strategyEntity=redisService.getValue(cacheKey);
        if(strategyEntity!=null)return strategyEntity;
        //没有就查找数据库
        Strategy strategy=strategyDao.queryStrategyByStrategyId(strategyId);
        strategyEntity=BeanUtil.copyProperties(strategy, StrategyEntity.class);
        //再添加进redis
        redisService.setValue(cacheKey,strategyEntity);
        return strategyEntity;

    }

    @Override
    public StrategyRuleEntity queryStrategyRuleEntity(Long strategyId, String ruleModel) {
        StrategyRule strategyRule=strategyRuleDao.queryStrategyRuleByStrategyIdWithRuleModel(strategyId,ruleModel);
        return BeanUtil.copyProperties(strategyRule,StrategyRuleEntity.class);
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRule strategyRule = new StrategyRule();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setAwardId(awardId);
        strategyRule.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRule);

    }

    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId) {
        StrategyAward strategyAward = new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        String ruleModels = strategyAwardDao.queryStrategyAwardRuleModels(strategyAward);
        return StrategyAwardRuleModelVO.builder().ruleModels(ruleModels).build();
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, String ruleModel) {
        return queryStrategyRuleValue(strategyId,null,ruleModel);
    }
}
