package com.flocier.domain.award.service;

import com.flocier.domain.award.model.entity.DistributeAwardEntity;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;

public interface IAwardService {
    void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity);
    /**
     * 配送发货奖品
     */
    void distributeAward(DistributeAwardEntity distributeAwardEntity);

}
