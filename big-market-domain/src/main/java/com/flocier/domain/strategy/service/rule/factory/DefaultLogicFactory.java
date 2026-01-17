package com.flocier.domain.strategy.service.rule.factory;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.service.annotation.LogicStrategy;
import com.flocier.domain.strategy.service.rule.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DefaultLogicFactory {

    public Map<String, ILogicFilter<?>>logicFilterMap=new ConcurrentHashMap<>();

    public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters){
        logicFilters.forEach(logic->{
            LogicStrategy strategy= AnnotationUtils.findAnnotation(logic.getClass(), LogicStrategy.class);
            if(strategy!=null)logicFilterMap.put(strategy.logicMode().getCode(),logic);
        });
    }
    public <T extends RaffleActionEntity.RaffleEntity> Map<String,ILogicFilter<T>> openLogicFilter(){
        //两层强制类型转换，让编译器知道这里没有问题，但我们要遵守约定
        return (Map<String, ILogicFilter<T>>) (Map<?,?>) logicFilterMap;
    }

    @Getter
    @AllArgsConstructor
    public enum LogicModel{
        RULE_WEIGHT("rule_weight","【抽奖前规则】根据抽奖权重返回可抽奖范围KEY"),
        RULE_BLACKLIST("rule_blacklist","【抽奖前规则】黑名单规则过滤，命中黑名单则直接返回"),
        ;

        private final String code;
        private final String info;
    }
}
