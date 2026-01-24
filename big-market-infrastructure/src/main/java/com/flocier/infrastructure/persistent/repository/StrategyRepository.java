package com.flocier.infrastructure.persistent.repository;

import cn.hutool.core.bean.BeanUtil;
import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.entity.StrategyRuleEntity;
import com.flocier.domain.strategy.model.vo.*;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.infrastructure.persistent.dao.*;
import com.flocier.infrastructure.persistent.po.*;
import com.flocier.infrastructure.persistent.redis.IRedisService;
import com.flocier.types.common.Constants;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Resource
    private IRuleTreeDao ruleTreeDao;
    @Resource
    private IRuleTreeNodeDao ruleTreeNodeDao;
    @Resource
    private IRuleTreeNodeLineDao ruleTreeNodeLineDao;
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

    @Override
    public RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
        //优先从缓存中获取数据
        String cacheKey=Constants.RedisKey.RULE_TREE_VO_KEY+treeId;
        RuleTreeVO ruleTreeVOCache=redisService.getValue(cacheKey);
        if(ruleTreeVOCache!=null)return ruleTreeVOCache;
        //将ruleModel作为id查询相应决策树数据
        RuleTree ruleTree=ruleTreeDao.queryRuleTreeByTreeId(treeId);
        List<RuleTreeNode> ruleTreeNodes=ruleTreeNodeDao.queryRuleTreeNodeListByTreeId(treeId);
        List<RuleTreeNodeLine> ruleTreeNodeLines=ruleTreeNodeLineDao.queryRuleTreeNodeLineListByTreeId(treeId);

        //填充ruleTreeNodeVO中的treeNodeLineList
        Map<String, List<RuleTreeNodeLineVO>> ruleTreeNodeLineMap =
                ruleTreeNodeLines.stream()
                        .map(line -> RuleTreeNodeLineVO.builder()
                                .treeId(line.getTreeId())
                                .ruleNodeFrom(line.getRuleNodeFrom())
                                .ruleNodeTo(line.getRuleNodeTo())
                                .ruleLimitType(RuleLimitTypeVO.valueOf(line.getRuleLimitType()))
                                .ruleLimitValue(RuleLogicCheckTypeVO.valueOf(line.getRuleLimitValue()))
                                .build())
                        .collect(Collectors.groupingBy(RuleTreeNodeLineVO::getRuleNodeFrom));

        //填充ruleTreeVO中的treeNodeMap
        Map<String,RuleTreeNodeVO>treeNodeVOMap=new HashMap<>();
        for (RuleTreeNode ruleTreeNode : ruleTreeNodes) {
            RuleTreeNodeVO ruleTreeNodeVO = RuleTreeNodeVO.builder()
                    .treeId(ruleTreeNode.getTreeId())
                    .ruleKey(ruleTreeNode.getRuleKey())
                    .ruleDesc(ruleTreeNode.getRuleDesc())
                    .ruleValue(ruleTreeNode.getRuleValue())
                    .treeNodeLineVOList(ruleTreeNodeLineMap.get(ruleTreeNode.getRuleKey()))
                    .build();
            treeNodeVOMap.put(ruleTreeNode.getRuleKey(), ruleTreeNodeVO);
        }
        //构建ruleTreeVO
        RuleTreeVO ruleTreeVODB = RuleTreeVO.builder()
                .treeId(ruleTree.getTreeId())
                .treeName(ruleTree.getTreeName())
                .treeDesc(ruleTree.getTreeDesc())
                .treeRootRuleNode(ruleTree.getTreeRootRuleKey())
                .treeNodeMap(treeNodeVOMap)
                .build();
        //加入缓存
        redisService.setValue(cacheKey,ruleTreeVODB);
        //包装并返回
        return ruleTreeVODB;
    }
}
