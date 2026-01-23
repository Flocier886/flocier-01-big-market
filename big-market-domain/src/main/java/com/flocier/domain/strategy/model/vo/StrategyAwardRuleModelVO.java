package com.flocier.domain.strategy.model.vo;

import com.flocier.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import com.flocier.types.common.Constants;
import lombok.*;

import java.util.Arrays;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class StrategyAwardRuleModelVO {
    String ruleModels;

    //过滤出对应奖品下ruleModel的抽奖中规则
    public String[] raffleCenterRuleModelList(){
        return Arrays.stream(ruleModels.split(Constants.SPLIT))
                .filter(DefaultLogicFactory.LogicModel::isCenter).toArray(String[]::new);
    }
}
