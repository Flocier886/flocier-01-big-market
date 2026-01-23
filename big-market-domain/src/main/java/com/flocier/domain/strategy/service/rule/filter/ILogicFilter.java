package com.flocier.domain.strategy.service.rule.filter;

import com.flocier.domain.strategy.model.entity.RaffleActionEntity;
import com.flocier.domain.strategy.model.entity.RuleMatterEntity;

public interface ILogicFilter <T extends RaffleActionEntity.RaffleEntity>{
    RaffleActionEntity<T> filter(RuleMatterEntity ruleMatterEntity);
}
