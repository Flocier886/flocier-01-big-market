package com.flocier.domain.strategy.service.armory;

import com.flocier.domain.strategy.model.entity.StrategyAwardEntity;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
public class StrategyArmory implements IStrategyArmory{

    @Resource
    private IStrategyRepository strategyRepository;
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        //获取策略配置
        List<StrategyAwardEntity> strategyAwardEntities=strategyRepository.queryStrategyAwardList(strategyId);
        //获取概率最小值
        BigDecimal minAwardRate=strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        //获取概率总和(一般为1)
        BigDecimal totalAwardRate=strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        //获取概率池坑位范围
        BigDecimal rateRange=totalAwardRate.divide(minAwardRate,0, RoundingMode.CEILING);
        log.info("rateRange值: {}",rateRange.intValue());
        //生成查找表（指需要在list集合中，存放上对应的奖品占位即可，占位越多等于概率越高）
        List<Integer> strategyAwardSearchRateTables=new ArrayList<>(rateRange.intValue());
        for (StrategyAwardEntity strategyAward:strategyAwardEntities){
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
        strategyRepository.storeStrategyAwardSearchRateTable(strategyId,shuffleStrategyAwardSearchRateTable.size(),shuffleStrategyAwardSearchRateTable);
        return true;
    }

    /**
     * 抽奖方法
     * */
    @Override
    public Integer getRandomAwardId(Long strategyId) {
        //获取指定策略概率表的离散数字范围值
        int rangeRate=strategyRepository.getRateRange(strategyId);
        //通过范围值随机生成数字查找策略概率表
        return strategyRepository.getStrategyAwardAssemble(strategyId,new SecureRandom().nextInt(rangeRate));
    }

}
