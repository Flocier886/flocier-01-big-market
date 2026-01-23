package com.flocier.domain.strategy.model.vo;

import com.flocier.domain.strategy.service.rule.factory.DefaultLogicFactory;
import com.flocier.types.common.Constants;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
