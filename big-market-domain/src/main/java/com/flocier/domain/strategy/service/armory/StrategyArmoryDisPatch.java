package com.flocier.domain.strategy.service.armory;

import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.entity.StrategyRuleEntity;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.types.common.Constants;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
public class StrategyArmoryDisPatch implements IStrategyArmory, IStrategyDisPatch {

    @Resource
    private IStrategyRepository strategyRepository;
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        //1.获取策略配置
        List<StrategyAwardEntity> strategyAwardEntities=strategyRepository.queryStrategyAwardList(strategyId);
        //缓存库存信息
        for(StrategyAwardEntity awardEntity:strategyAwardEntities){
            Integer awardId=awardEntity.getAwardId();
            Integer awardCount=awardEntity.getAwardCount();
            cacheStrategyAwardCount(strategyId,awardId,awardCount);
        }
        assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardEntities);

        //2.权重策略配置（在原本默认配置基础上再加上多的权重配置）
        //.1 查询策略的权重模型
        StrategyEntity strategyEntity=strategyRepository.queryStrategyEntityByStrategyId(strategyId);
        String ruleWeight=strategyEntity.getRuleWeight();
        if(ruleWeight==null)return true;
        //.2 查询对应策略规则表中是否有都对应规则
        StrategyRuleEntity strategyRuleEntity=strategyRepository.queryStrategyRuleEntity(strategyId,ruleWeight);
        if(strategyRuleEntity==null){
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(),ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
        }
        //.3 有的话就将规则中的数据全部添加新的概率策略表
        Map<String,List<Integer>>ruleWeightValuesMap=strategyRuleEntity.getRuleWeightValues();
        Set<String> keys=ruleWeightValuesMap.keySet();
        for(String key:keys){
            List<Integer> ruleWeightValues=ruleWeightValuesMap.get(key);
            ArrayList<StrategyAwardEntity>strategyAwardEntitiesClone=new ArrayList<>(strategyAwardEntities);
            //删除规则数据中没有的奖品号
            strategyAwardEntitiesClone.removeIf(entity -> !ruleWeightValues.contains(entity.getAwardId()));
            assembleLotteryStrategy(String.valueOf(strategyId).concat("_").concat(key),strategyAwardEntitiesClone);
        }
        return true;
    }

    @Override
    public void assembleLotteryStrategyByActivityId(Long activityId) {
        Long strategyId=strategyRepository.queryStrategyIdByActivityId(activityId);
        assembleLotteryStrategy(strategyId);
    }

    /**
     * 该方法用于创建指定的strategyAwardEntities的对应概率抽奖池
     * */
    private void assembleLotteryStrategy(String key, List<StrategyAwardEntity> strategyAwardEntities) {
        //获取概率最小值
        BigDecimal minAwardRate= strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        //获取概率总和(一般为1)
        BigDecimal totalAwardRate= strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        //获取概率池坑位范围
        BigDecimal rateRange=totalAwardRate.divide(minAwardRate,0, RoundingMode.CEILING);
        log.info("rateRange值: {}",rateRange.intValue());
        //生成查找表（指需要在list集合中，存放上对应的奖品占位即可，占位越多等于概率越高）
        List<Integer> strategyAwardSearchRateTables=new ArrayList<>(rateRange.intValue());
        for (StrategyAwardEntity strategyAward: strategyAwardEntities){
            Integer awardId=strategyAward.getAwardId();
            BigDecimal awardRate=strategyAward.getAwardRate();
            //对于每个奖品填充对应位数的离散化概率位数
            for(int i=0;i<rateRange.multiply(awardRate).setScale(0,RoundingMode.CEILING).intValue();i++){
                strategyAwardSearchRateTables.add(awardId);
            }
        }
        //打乱顺序
        Collections.shuffle(strategyAwardSearchRateTables);
        log.info("strategyAwardSearchRateTables的大小: {}",strategyAwardSearchRateTables.size());
        //重新生成hash集合表
        Map<Integer,Integer>shuffleStrategyAwardSearchRateTable=new LinkedHashMap<>();
        for (int i=0;i<strategyAwardSearchRateTables.size();i++){
            shuffleStrategyAwardSearchRateTable.put(i,strategyAwardSearchRateTables.get(i));
            //log.info("key值: {},value值: {}",i,strategyAwardSearchRateTables.get(i));
        }
        //存放至redis中
        strategyRepository.storeStrategyAwardSearchRateTable(key,shuffleStrategyAwardSearchRateTable.size(),shuffleStrategyAwardSearchRateTable);
    }

    /**
     * 抽奖方法
     * */
    @Override
    public Integer getRandomAwardId(Long strategyId) {
        //获取指定策略概率表的离散数字范围值
        int rangeRate=strategyRepository.getRateRange(strategyId);
        //通过范围值随机生成数字查找策略概率表
        return strategyRepository.getStrategyAwardAssemble(String.valueOf(strategyId),new SecureRandom().nextInt(rangeRate));
    }

    @Override
    public Integer getRandomAwardId(Long strategyId,String ruleWeightValue) {
        String key=String.valueOf(strategyId).concat("_").concat(ruleWeightValue);
        //获取指定策略概率表的离散数字范围值
        int rangeRate=strategyRepository.getRateRange(key);
        //通过范围值随机生成数字查找策略概率表
        return strategyRepository.getStrategyAwardAssemble(key,new SecureRandom().nextInt(rangeRate));
    }

    @Override
    public void cacheStrategyAwardCount(Long strategyId,Integer awardId, Integer awardCount) {
        String cacheKey= Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY+strategyId+Constants.UNDERLINE+awardId;
        strategyRepository.cacheStrategyAwardCount(cacheKey,awardCount);
    }

    @Override
    public Boolean subtractionAwardStock(Long strategyId, Integer awardId, Date endDateTime) {
        String cacheKey=Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY+strategyId+Constants.UNDERLINE+awardId;
        return strategyRepository.subtractionAwardStock(cacheKey,endDateTime);
    }
}
