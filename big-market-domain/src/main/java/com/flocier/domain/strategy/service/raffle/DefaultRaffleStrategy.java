package com.flocier.domain.strategy.service.raffle;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RaffleFactorEntity;
import com.flocier.domain.strategy.model.entity.RuleMatterEntity;
import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import com.flocier.domain.strategy.service.rule.ILogicFilter;
import com.flocier.domain.strategy.service.rule.factory.DefaultLogicFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy{
    @Resource
    private DefaultLogicFactory logicFactory;

    public DefaultRaffleStrategy(IStrategyRepository repository, IStrategyDisPatch strategyDispatch) {
        super(repository, strategyDispatch);
    }

    @Override
    protected RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity raffleFactorEntity, String... logics) {
        //获取过滤器
        Map<String, ILogicFilter<RaffleActionEntity.BeforeRaffleEntity>>logicFilterMap=logicFactory.openLogicFilter();

        //判断是否有黑名单过滤，优先过滤黑名单
        String ruleBlackList= Arrays.stream(logics)
                .filter(str->str.contains(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode()))
                .findFirst()
                .orElse(null);
        if(ruleBlackList!=null){
            //logics中有黑名单规则
            ILogicFilter<RaffleActionEntity.BeforeRaffleEntity> blackListFilter=logicFilterMap.get(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode());
            RuleMatterEntity ruleMatterEntity=RuleMatterEntity.builder()
                    .strategyId(raffleFactorEntity.getStrategyId())
                    .userId(raffleFactorEntity.getUserId())
                    .ruleModel(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode())
                    .build();
            RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity> actionEntity=blackListFilter.filter(ruleMatterEntity);
            if(!actionEntity.getCode().equals(RuleLogicCheckTypeVO.ALLOW.getCode())){
                return actionEntity;
            }
        }
        //按logics中的规则对比过滤其他规则
        List<String>ruleList= Arrays.stream(logics)
                .filter(str->!str.equals(DefaultLogicFactory.LogicModel.RULE_BLACKLIST.getCode()))
                .collect(Collectors.toList());
        RaffleActionEntity<RaffleActionEntity.BeforeRaffleEntity>actionEntity=null;
        for (String ruleModel:ruleList){
            ILogicFilter<RaffleActionEntity.BeforeRaffleEntity> logicFilter=logicFilterMap.get(ruleModel);
            RuleMatterEntity ruleMatterEntity=RuleMatterEntity.builder()
                    .ruleModel(ruleModel)
                    .userId(raffleFactorEntity.getUserId())
                    .strategyId(raffleFactorEntity.getStrategyId())
                    .build();
            actionEntity=logicFilter.filter(ruleMatterEntity);
            log.info("抽奖前规则过滤 userId: {} ruleModel: {} code: {} info: {}", raffleFactorEntity.getUserId(), ruleModel, actionEntity.getCode(),actionEntity.getInfo());
            if(!actionEntity.getCode().equals(RuleLogicCheckTypeVO.ALLOW.getCode()))return actionEntity;
        }
        return actionEntity;
    }
}
