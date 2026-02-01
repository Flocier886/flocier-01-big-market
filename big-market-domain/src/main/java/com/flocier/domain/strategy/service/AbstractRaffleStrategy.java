package com.flocier.domain.strategy.service;

import com.flocier.domain.strategy.model.entity.*;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.chain.ILogicChain;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.flocier.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {
    protected IStrategyRepository repository;
    protected IStrategyDisPatch strategyDispatch;
    protected final DefaultChainFactory defaultChainFactory;
    protected final DefaultTreeFactory defaultTreeFactory;

    protected AbstractRaffleStrategy(IStrategyRepository repository,IStrategyDisPatch strategyDispatch,DefaultChainFactory defaultChainFactory, DefaultTreeFactory defaultTreeFactory) {
        this.repository=repository;
        this.strategyDispatch=strategyDispatch;
        this.defaultChainFactory = defaultChainFactory;
        this.defaultTreeFactory = defaultTreeFactory;
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
        DefaultChainFactory.StrategyAwardVO chainStrategyAwardVO=raffleLogicChain(userId,strategyId);
        log.info("抽奖策略计算-责任链 {} {} {} {}", userId, strategyId, chainStrategyAwardVO.getAwardId(), chainStrategyAwardVO.getLogicModel());
        //黑名单直接跳过后面的过滤
        if(chainStrategyAwardVO.getLogicModel().equals(DefaultChainFactory.LogicModel.RULE_BLACKLIST.getCode()))
            // TODO awardConfig 暂时为空。黑名单指定积分奖品，后续需要在库表中配置上对应的1积分值，并获取到。
            return buildRaffleAwardEntity(strategyId,chainStrategyAwardVO.getAwardId(),chainStrategyAwardVO.getAwardRuleValue());

        //抽奖中的规则过滤
        DefaultTreeFactory.StrategyAwardVO treeStrategyAwardVO=raffleLogicTree(userId,strategyId,chainStrategyAwardVO.getAwardId(),raffleFactorEntity.getEndDateTime());
        log.info("抽奖策略计算-规则树 {} {} {} {}", userId, strategyId, treeStrategyAwardVO.getAwardId(), treeStrategyAwardVO.getAwardRuleValue());
        return buildRaffleAwardEntity(strategyId,treeStrategyAwardVO.getAwardId(),treeStrategyAwardVO.getAwardRuleValue());
    }

    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId, Date endDateTime);

    private RaffleAwardEntity buildRaffleAwardEntity(Long strategyId, Integer awardId, String awardConfig) {
        StrategyAwardEntity strategyAward = repository.queryStrategyAwardEntity(strategyId, awardId);
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .awardConfig(awardConfig)
                .awardTitle(strategyAward.getAwardTitle())
                .sort(strategyAward.getSort())
                .build();
    }


    protected abstract DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId,Long strategyId);
    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId,Long strategyId,Integer awardId);
}
