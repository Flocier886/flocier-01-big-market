package com.flocier.domain.strategy.model.entity;

import com.flocier.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyRuleEntity {
    /**抽奖策略ID*/
    private Long strategyId;
    /**抽奖奖品ID【规则类型为策略，则不需要奖品ID】*/
    private Integer awardId;
    /**抽象规则类型；1-策略规则、2-奖品规则*/
    private Integer ruleType;
    /**抽奖规则类型【rule_random - 随机值计算、rule_lock - 抽奖几次后解锁、rule_luck_award - 幸运奖(兜底奖品)】*/
    private String ruleModel;
    /**抽奖规则比值*/
    private String ruleValue;
    /**抽奖规则描述*/
    private String ruleDesc;


    /**
     * 解析数据库中ruleValue字符串
     * */
    public Map<String, List<Integer>> getRuleWeightValues() {
        if(!"rule_weight".equals(ruleModel))return null;
        String[] ruleValueGroups=ruleValue.split(Constants.SPACE);
        Map<String,List<Integer>>resultMap=new HashMap<>();
        for (String group:ruleValueGroups){
            if(group==null || group.isEmpty())return resultMap;
            String[] parts=group.split(Constants.COLON);
            if(parts.length!=2){
                throw new IllegalArgumentException("rule_weight rule_rule invalid input format" + group);
            }
            String[] valueStrings=parts[1].split(Constants.SPLIT);
            List<Integer>values= Arrays.stream(valueStrings)
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            resultMap.put(group,values);
        }
        return resultMap;
    }
}
