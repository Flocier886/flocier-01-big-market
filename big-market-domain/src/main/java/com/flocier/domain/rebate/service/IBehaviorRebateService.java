package com.flocier.domain.rebate.service;

import com.flocier.domain.rebate.model.entity.BehaviorEntity;

import java.util.List;

public interface IBehaviorRebateService {
    List<String> createOrder(BehaviorEntity behaviorEntity);
}
