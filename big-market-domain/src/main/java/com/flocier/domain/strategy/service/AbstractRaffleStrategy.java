package com.flocier.domain.strategy.service;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RaffleAwardEntity;
import com.flocier.domain.strategy.model.entity.RaffleFactorEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.chain.ILogicChain;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {
    protected IStrategyRepository repository;
    protected IStrategyDisPatch strategyDispatch;
    private final DefaultChainFactory defaultChainFactory;
    public AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDisPatch strategyDispatch, DefaultChainFactory defaultChainFactory) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
        this.defaultChainFactory=defaultChainFactory;
    }


    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        //参数校验
        Long strategyId=raffleFactorEntity.getStrategyId();
        String userId= raffleFactorEntity.getUserId();
        if(strategyId==null || StringUtils.isBlank(userId)){
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        //抽奖前的规则过滤
        ILogicChain logicChain=defaultChainFactory.openLogicChain(strategyId);
        Integer awardId=logicChain.logic(userId,strategyId);
        //抽奖中的规则过滤
        StrategyAwardRuleModelVO strategyAwardRuleModelVO=repository.queryStrategyAwardRuleModelVO(strategyId,awardId);
        RaffleActionEntity<RaffleActionEntity.CenterRaffleEntity> centerActionEntity=this.doCheckRaffleCenterLogic(RaffleFactorEntity.builder()
                .strategyId(strategyId)
                .userId(userId)
                .awardId(awardId)
                .build(),strategyAwardRuleModelVO.raffleCenterRuleModelList());
        if(centerActionEntity.getCode().equals(RuleLogicCheckTypeVO.TAKE_OVER.getCode())){
            log.info("【临时日志】中奖中规则拦截，通过抽奖后规则 rule_luck_award 走兜底奖励。");
            return RaffleAwardEntity.builder()
                    .awardDesc("中奖中规则拦截，通过抽奖后规则 rule_luck_award 走兜底奖励。")
                    .build();
        }
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .build();
    }

    protected abstract RaffleActionEntity<RaffleActionEntity.CenterRaffleEntity>doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity,String... logics);
}
