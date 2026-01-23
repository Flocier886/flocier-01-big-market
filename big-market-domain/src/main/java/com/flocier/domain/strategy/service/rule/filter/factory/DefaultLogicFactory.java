package com.flocier.domain.strategy.service.rule.filter.factory;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.service.annotation.LogicStrategy;
import com.flocier.domain.strategy.service.rule.filter.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        RULE_WEIGHT("rule_weight","【抽奖前规则】根据抽奖权重返回可抽奖范围KEY","before"),
        RULE_BLACKLIST("rule_blacklist","【抽奖前规则】黑名单规则过滤，命中黑名单则直接返回","before"),
        RULE_LOCK("rule_lock", "【抽奖中规则】抽奖n次后，对应奖品可解锁抽奖", "center"),
        RULE_LUCK_AWARD("rule_luck_award", "【抽奖后规则】抽奖n次后，对应奖品可解锁抽奖", "after"),

        ;

        private final String code;
        private final String info;
        private final String type;

        public static boolean isCenter(String ruleModel) {
            return "center".equals(LogicModel.valueOf(ruleModel.toUpperCase()).type);
        }
        public static boolean isAfter(String ruleModel) {
            return "after".equals(LogicModel.valueOf(ruleModel.toUpperCase()).type);
        }
    }
}
