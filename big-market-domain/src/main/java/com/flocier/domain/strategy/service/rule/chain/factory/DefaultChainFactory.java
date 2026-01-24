package com.flocier.domain.strategy.service.rule.chain.factory;

import com.flocier.domain.strategy.model.entity.StrategyEntity;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.rule.chain.ILogicChain;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultChainFactory {
    private Map<String, ILogicChain> logicChainMap;
    protected IStrategyRepository repository;
    public DefaultChainFactory(Map<String, ILogicChain> logicChainMap, IStrategyRepository repository) {
        this.logicChainMap = logicChainMap;
        this.repository = repository;
    }

    public ILogicChain openLogicChain(Long strategyId){
        //先查询策略信息
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        String[] ruleModels = strategy.ruleModels();
        //若没有设置规则，就返回默认链
        if(ruleModels==null || ruleModels.length==0)return logicChainMap.get("default");
        //根据策略的规则创建对应的过滤链
        ILogicChain logicChain=logicChainMap.get(ruleModels[0]);
        ILogicChain current=logicChain;
        for (int i=0;i<ruleModels.length;i++){
            ILogicChain nextChain=logicChainMap.get(ruleModels[i]);
            current=current.appendNext(nextChain);
        }
        //在责任链的最后装默认链作为兜底
        current.appendNext(logicChainMap.get("default"));
        //返回过滤链的头部
        return logicChain;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyAwardVO{
        /** 抽奖奖品ID - 内部流转使用 */
        private Integer awardId;
        /**  */
        private String logicModel;
    }
    @Getter
    @AllArgsConstructor
    public enum LogicModel{
        //身份牌
        RULE_DEFAULT("rule_default", "默认抽奖"),
        RULE_BLACKLIST("rule_blacklist", "黑名单抽奖"),
        RULE_WEIGHT("rule_weight", "权重规则"),
        ;

        private final String code;
        private final String info;

        }
}
