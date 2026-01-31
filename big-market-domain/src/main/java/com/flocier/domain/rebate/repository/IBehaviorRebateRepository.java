package com.flocier.domain.rebate.repository;

import com.flocier.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import com.flocier.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import com.flocier.domain.rebate.model.vo.BehaviorTypeVO;
import com.flocier.domain.rebate.model.vo.DailyBehaviorRebateVO;

import java.util.List;

public interface IBehaviorRebateRepository {
    List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(BehaviorTypeVO behaviorTypeVO);

    void saveUserRebateRecord(String userId,List<BehaviorRebateAggregate> behaviorRebateAggregates);

    List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo);
}
