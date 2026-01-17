package com.flocier.domain.strategy.model.entity;

import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleActionEntity <T extends RaffleActionEntity.RaffleEntity>{
    private String code= RuleLogicCheckTypeVO.ALLOW.getCode();
    private String info=RuleLogicCheckTypeVO.ALLOW.getInfo();
    private String ruleModel;
    private T data;

    //空类，作用是限制模板类对象必须为指定的类
    static public class RaffleEntity{
    }

    //指定类1号
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static public class BeforeRaffleEntity extends RaffleEntity{
        /**
         * 策略ID
         */
        private Long strategyId;
        /**
         * 权重值Key；用于抽奖时可以选择权重抽奖。
         */
        private String ruleWeightValueKey;
        /**
         * 奖品ID；
         */
        private Integer awardId;
    }
    //指定类2号
    static public class CenterRaffleEntity extends RaffleEntity{

    }
    //指定类3号
    static public class AfterRaffleEntity extends RaffleEntity{

    }
}
