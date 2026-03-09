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
        log.info("抽奖策略计算-责任链 {} {} {} {} {}", userId, strategyId, chainStrategyAwardVO.getAwardId(), chainStrategyAwardVO.getLogicModel(),chainStrategyAwardVO.getAwardRuleValue());
        //黑名单直接跳过后面的过滤
        if(chainStrategyAwardVO.getLogicModel().equals(DefaultChainFactory.LogicModel.RULE_BLACKLIST.getCode()))
            //黑名单奖品不会配置在strategy_award中，而是配置在award表中，所以填写相关信息不能再查strategy_award表
            return buildBlacklistRaffleAwardEntity(chainStrategyAwardVO.getAwardId(),chainStrategyAwardVO.getAwardRuleValue());

        //抽奖中的规则过滤
        DefaultTreeFactory.StrategyAwardVO treeStrategyAwardVO=raffleLogicTree(userId,strategyId,chainStrategyAwardVO.getAwardId(),raffleFactorEntity.getEndDateTime());
        //这里的awardValue值可能会被决策树过滤掉，只需要在到时候做奖品发放处理时再对其对应奖品进行一次关于award表查询即可（简单来说就是award表里面有awardValue备份）
        log.info("抽奖策略计算-规则树 {} {} {} {}", userId, strategyId, treeStrategyAwardVO.getAwardId(), treeStrategyAwardVO.getAwardRuleValue());
        return buildRaffleAwardEntity(strategyId,treeStrategyAwardVO.getAwardId(),treeStrategyAwardVO.getAwardRuleValue());
    }

    private RaffleAwardEntity buildBlacklistRaffleAwardEntity(Integer awardId, String awardRuleValue) {
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .awardConfig(awardRuleValue)
                .awardTitle("黑名单奖品")
                .sort(0)
                .build();
    }

    private RaffleAwardEntity buildRaffleAwardEntity(Long strategyId, Integer awardId, String awardConfig) {
        StrategyAwardEntity strategyAward = repository.queryStrategyAwardEntity(strategyId, awardId);
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .awardConfig(awardConfig)
                .awardTitle(strategyAward.getAwardTitle())
                .sort(strategyAward.getSort())
                .build();
    }

    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId, Date endDateTime);
    protected abstract DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId,Long strategyId);
    protected abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId,Long strategyId,Integer awardId);
}
