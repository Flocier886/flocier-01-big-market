package com.flocier.domain.award.service.distribute;

import com.flocier.domain.award.model.entity.DistributeAwardEntity;

public interface IDistributeAward {
    void giveOutPrizes(DistributeAwardEntity distributeAwardEntity);
}
