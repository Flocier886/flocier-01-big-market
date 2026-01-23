package com.flocier.domain.strategy.service.raffle;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RaffleFactorEntity;
import com.flocier.domain.strategy.model.entity.RuleMatterEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.AbstractRaffleStrategy;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.domain.strategy.service.rule.filter.ILogicFilter;
import com.flocier.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {
    @Resource
    private DefaultLogicFactory logicFactory;

    public DefaultRaffleStrategy(IStrategyRepository repository, IStrategyDisPatch strategyDispatch, DefaultChainFactory defaultChainFactory) {
        super(repository, strategyDispatch,defaultChainFactory);
    }

    @Override
    protected RaffleActionEntity<RaffleActionEntity.CenterRaffleEntity> doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity, String... logics) {
        if (logics == null || 0 == logics.length) return RaffleActionEntity.<RaffleActionEntity.CenterRaffleEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
        Map<String,ILogicFilter<RaffleActionEntity.CenterRaffleEntity>>logicFilterMap=logicFactory.openLogicFilter();
        RaffleActionEntity<RaffleActionEntity.CenterRaffleEntity>actionEntity=null;
        for (String ruleModel:logics){
            ILogicFilter<RaffleActionEntity.CenterRaffleEntity> logicFilter=logicFilterMap.get(ruleModel);
            RuleMatterEntity ruleMatterEntity=RuleMatterEntity.builder()
                    .ruleModel(ruleModel)
                    .userId(raffleFactorEntity.getUserId())
                    .strategyId(raffleFactorEntity.getStrategyId())
                    .awardId(raffleFactorEntity.getAwardId())
                    .build();
            actionEntity=logicFilter.filter(ruleMatterEntity);
            log.info("抽奖中规则过滤 userId: {} ruleModel: {} awardId: {} code: {} info: {}", raffleFactorEntity.getUserId(), ruleModel, raffleFactorEntity.getAwardId(),actionEntity.getCode(),actionEntity.getInfo());
            if(!actionEntity.getCode().equals(RuleLogicCheckTypeVO.ALLOW.getCode()))return actionEntity;
        }
        return actionEntity;
    }
}
