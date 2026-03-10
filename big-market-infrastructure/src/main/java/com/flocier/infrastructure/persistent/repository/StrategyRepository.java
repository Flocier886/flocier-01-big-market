package com.flocier.infrastructure.persistent.repository;

import cn.hutool.core.bean.BeanUtil;
import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.entity.StrategyRuleEntity;
import com.flocier.domain.strategy.model.vo.*;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.infrastructure.persistent.dao.*;
import com.flocier.infrastructure.persistent.po.*;
import com.flocier.infrastructure.persistent.redis.IRedisService;
import com.flocier.types.common.Constants;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.flocier.types.enums.ResponseCode.UN_ASSEMBLED_STRATEGY_ARMORY;

@Slf4j
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
    @Resource
    private IRaffleActivityDao raffleActivityDao;
    @Resource
    private IRaffleActivityAccountDayDao raffleActivityAccountDayDao;
    @Resource
    private IRaffleActivityAccountDao raffleActivityAccountDao;
    @Resource
    private IAwardDao awardDao;

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        //先从redis中查找获取
        String cacheKey= Constants.RedisKey.STRATEGY_AWARD_LIST_KEY+strategyId;
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
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key;
        if (!redisService.isExists(cacheKey)) {
            throw new AppException(UN_ASSEMBLED_STRATEGY_ARMORY.getCode(), cacheKey + Constants.COLON + UN_ASSEMBLED_STRATEGY_ARMORY.getInfo());
        }
        return redisService.getValue(cacheKey);

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

    /**
     * 初始化抽奖池时先尝试进行库存缓存
     * */
    @Override
    public void cacheStrategyAwardCount(String cacheKey, Integer awardCount) {
        if(redisService.isExists(cacheKey))return;
        redisService.setAtomicLong(cacheKey,awardCount);
    }

    /**
     * 在缓存中扣减库存并上锁
     * */
    @Override
    public Boolean subtractionAwardStock(String cacheKey) {
        return subtractionAwardStock(cacheKey,null);
    }

    /**
     * 获取延迟队列和阻塞队列，为延迟队列发送消息
     * */
    @Override
    public void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO) {
        String cacheKey=Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);
        RDelayedQueue<StrategyAwardStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);
        //发送给延迟队列消息,延迟3秒
        delayedQueue.offer(strategyAwardStockKeyVO,3, TimeUnit.SECONDS);
    }

    /**
     * 从阻塞队列中弹出信息
     * */
    @Override
    public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException{
        String cacheKey=Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
        RBlockingQueue<StrategyAwardStockKeyVO>destinationQueue=redisService.getBlockingQueue(cacheKey);
        return destinationQueue.poll();
    }

    @Override
    public StrategyAwardStockKeyVO takeQueueValue(Long strategyId, Integer awardId) throws InterruptedException {
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY + Constants.UNDERLINE + strategyId + Constants.UNDERLINE + awardId;
        RBlockingQueue<StrategyAwardStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
        return destinationQueue.poll();

    }

    /**
     * 更新数据库库存信息
     * */
    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId){
        StrategyAward strategyAward = new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        strategyAwardDao.updateStrategyAwardStock(strategyAward);

    }

    @Override
    public StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId + Constants.UNDERLINE + awardId;
        StrategyAwardEntity strategyAwardEntity = redisService.getValue(cacheKey);
        if (null != strategyAwardEntity) return strategyAwardEntity;
        // 查询数据
        StrategyAward strategyAwardReq = new StrategyAward();
        strategyAwardReq.setStrategyId(strategyId);
        strategyAwardReq.setAwardId(awardId);
        StrategyAward strategyAwardRes = strategyAwardDao.queryStrategyAward(strategyAwardReq);
        // 转换数据
        strategyAwardEntity = StrategyAwardEntity.builder()
                .strategyId(strategyAwardRes.getStrategyId())
                .awardId(strategyAwardRes.getAwardId())
                .awardTitle(strategyAwardRes.getAwardTitle())
                .awardSubtitle(strategyAwardRes.getAwardSubtitle())
                .awardCount(strategyAwardRes.getAwardCount())
                .awardCountSurplus(strategyAwardRes.getAwardCountSurplus())
                .awardRate(strategyAwardRes.getAwardRate())
                .sort(strategyAwardRes.getSort())
                .build();
        // 缓存结果
        redisService.setValue(cacheKey, strategyAwardEntity);
        // 返回数据
        return strategyAwardEntity;

    }

    @Override
    public Long queryStrategyIdByActivityId(Long activityId) {
        return raffleActivityDao.queryStrategyIdByActivityId(activityId);
    }

    @Override
    public Integer queryTodayUserRaffleCount(String userId, Long strategyId) {
        //获取活动id
        Long activityId = raffleActivityDao.queryActivityIdByStrategyId(strategyId);
        //查询并返回
        RaffleActivityAccountDay raffleActivityAccountDayReq=new RaffleActivityAccountDay();
        raffleActivityAccountDayReq.setUserId(userId);
        raffleActivityAccountDayReq.setActivityId(activityId);
        raffleActivityAccountDayReq.setDay(raffleActivityAccountDayReq.currentDay());
        RaffleActivityAccountDay raffleActivityAccountDay = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayReq);
        //如果为空，证明当天还没有过抽奖次数
        if(raffleActivityAccountDay==null)return 0;
        //当天抽奖次数等于总次数减去剩余次数
        return raffleActivityAccountDay.getDayCount()-raffleActivityAccountDay.getDayCountSurplus();
    }

    @Override
    public Map<String, Integer> queryAwardRuleLockCount(String[] treeIds) {
        if(treeIds==null && treeIds.length==0)return new HashMap<>();
        List<RuleTreeNode> ruleTreeNodes=ruleTreeNodeDao.queryRuleLocks(treeIds);
        Map<String,Integer>resultMap=new HashMap<>();
        for(RuleTreeNode ruleTreeNode:ruleTreeNodes){
            String treeId=ruleTreeNode.getTreeId();
            Integer ruleValue=Integer.valueOf(ruleTreeNode.getRuleValue());
            resultMap.put(treeId,ruleValue);
        }
        return resultMap;
    }

    @Override
    public Boolean subtractionAwardStock(String cacheKey, Date endDateTime) {
        //原子性的操作缓存-1
        long surplus=redisService.decr(cacheKey);
        if(surplus<0){
            //库存不足
            redisService.setValue(cacheKey,0);
            return false;
        }
        //操作成功，上锁防止超卖
        String lockKey=cacheKey+Constants.UNDERLINE+surplus;
        Boolean lock=false;
        if(endDateTime==null){
            lock=redisService.setNx(lockKey);
        } else {
            long expireMillis=endDateTime.getTime()-System.currentTimeMillis()+TimeUnit.DAYS.toMillis(1);
            lock=redisService.setNx(lockKey,expireMillis,TimeUnit.MILLISECONDS);
        }
        if(!lock){
            log.info("策略奖品库存加锁失败 {}", lockKey);
        }
        return lock;
    }

    @Override
    public Integer queryActivityAccountTotalUseCount(String userId, Long strategyId) {
        Long activityId=raffleActivityDao.queryActivityIdByStrategyId(strategyId);
        RaffleActivityAccount raffleActivityAccount = raffleActivityAccountDao.queryActivityAccountByUserId(RaffleActivityAccount.builder()
                .userId(userId)
                .activityId(activityId)
                .build());
        return raffleActivityAccount.getTotalCount() - raffleActivityAccount.getTotalCountSurplus();
    }

    @Override
    public List<RuleWeightVO> queryAwardRuleWeight(Long strategyId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.STRATEGY_RULE_WEIGHT_KEY + strategyId;
        List<RuleWeightVO> ruleWeightVOS = redisService.getValue(cacheKey);
        if (null != ruleWeightVOS) return ruleWeightVOS;
        ruleWeightVOS = new ArrayList<>();
        // 查询权重规则配置
        StrategyRule strategyRuleReq = new StrategyRule();
        strategyRuleReq.setStrategyId(strategyId);
        strategyRuleReq.setRuleModel(DefaultChainFactory.LogicModel.RULE_WEIGHT.getCode());
        String ruleValue = strategyRuleDao.queryStrategyRuleValue(strategyRuleReq);
        // 借助实体对象转换规则
        StrategyRuleEntity strategyRuleEntity = new StrategyRuleEntity();
        strategyRuleEntity.setRuleModel(DefaultChainFactory.LogicModel.RULE_WEIGHT.getCode());
        strategyRuleEntity.setRuleValue(ruleValue);
        Map<String, List<Integer>> ruleWeightValues = strategyRuleEntity.getRuleWeightValues();
        // 遍历规则组装奖品配置
        Set<String> ruleWeightKeys = ruleWeightValues.keySet();
        for (String ruleWeightKey : ruleWeightKeys) {
            List<Integer> awardIds = ruleWeightValues.get(ruleWeightKey);
            List<RuleWeightVO.Award> awardList = new ArrayList<>();
            for (Integer awardId : awardIds) {
                StrategyAward strategyAwardReq = new StrategyAward();
                strategyAwardReq.setStrategyId(strategyId);
                strategyAwardReq.setAwardId(awardId);
                StrategyAward strategyAward = strategyAwardDao.queryStrategyAward(strategyAwardReq);
                awardList.add(RuleWeightVO.Award.builder()
                        .awardId(strategyAward.getAwardId())
                        .awardTitle(strategyAward.getAwardTitle())
                        .build());
            }

            ruleWeightVOS.add(RuleWeightVO.builder()
                    .ruleValue(ruleValue)
                    .weight(Integer.valueOf(ruleWeightKey.split(Constants.COLON)[0]))
                    .awardIds(awardIds)
                    .awardList(awardList)
                    .build());
        }

        // 设置缓存 - 实际场景中，这类数据，可以在活动下架的时候统一清空缓存。
        redisService.setValue(cacheKey, ruleWeightVOS);

        return ruleWeightVOS;

    }

    @Override
    public String queryStrategyAwardValue(Integer awardId) {
        return awardDao.queryAwardConfigByAwardId(awardId);
    }

    @Override
    public List<StrategyAwardStockKeyVO> queryOpenActivityStrategyAwardList() {
        List<StrategyAward> strategyAwards = strategyAwardDao.queryOpenActivityStrategyAwardList();
        if (null == strategyAwards || strategyAwards.isEmpty()) return null;

        List<StrategyAwardStockKeyVO> strategyAwardStockKeyVOS = new ArrayList<>();
        for (StrategyAward strategyAward: strategyAwards){
            StrategyAwardStockKeyVO strategyAwardStockKeyVO = StrategyAwardStockKeyVO.builder()
                    .strategyId(strategyAward.getStrategyId())
                    .awardId(strategyAward.getAwardId())
                    .build();
            strategyAwardStockKeyVOS.add(strategyAwardStockKeyVO);
        }

        return strategyAwardStockKeyVOS;

    }


}
