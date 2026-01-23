package com.flocier.domain.strategy.service.raffle;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RaffleAwardEntity;
import com.flocier.domain.strategy.model.entity.RaffleFactorEntity;
import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.IRaffleStrategy;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.factory.DefaultLogicFactory;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {
    protected IStrategyRepository repository;
    protected IStrategyDisPatch strategyDispatch;

    public AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDisPatch strategyDispatch) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
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
        //查询策略
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity>actionEntity=doCheckRaffleBeforeLogic(raffleFactorEntity,strategy.ruleModels());
        //看actionEntity中的code是否为TAKE_OVER的code，表明它被过滤过了
        if(actionEntity.getCode().equals(RuleLogicCheckTypeVO.TAKE_OVER.getCode())){
            //如果ruleModel是RULE_BLACKLIST的code，表明被黑名单规则拦截
            if(actionEntity.getRuleModel().equals(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())){
                return RaffleAwardEntity.builder()
                        .awardId(actionEntity.getData().getAwardId())
                        .build();
            } else if (actionEntity.getRuleModel().equals(DefaultLogicFactory.LogicModel.RULE_WEIGHT.getCode())){
                //表明被权重规则拦截,实行抽奖逻辑
                RaffleActionEntity.BeforeRaffleEntity beforeRaffleEntity=actionEntity.getData();
                String ruleWeightValueKey= beforeRaffleEntity.getRuleWeightValueKey();
                Integer awardId=strategyDispatch.getRandomAwardId(strategyId,ruleWeightValueKey);
                return RaffleAwardEntity.builder()
                        .awardId(awardId)
                        .build();
            }
        }

        //默认抽奖逻辑
        Integer awardId=strategyDispatch.getRandomAwardId(strategyId);
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
    protected abstract RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity>doCheckRaffleBeforeLogic(RaffleFactorEntity raffleFactorEntity,String... logics);

    protected abstract RaffleActionEntity<RaffleActionEntity.CenterRaffleEntity>doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity,String... logics);
}
