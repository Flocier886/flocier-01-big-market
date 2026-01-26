package com.flocier.domain.activity.service.rule;

import com.flocier.domain.activity.model.entity.ActivityCountEntity;
import com.flocier.domain.activity.model.entity.ActivityEntity;
import com.flocier.domain.activity.model.entity.ActivitySkuEntity;

public interface IActionChain extends IActionChainArmory{
    boolean action(ActivityEntity activityEntity, ActivityCountEntity activityCountEntity, ActivitySkuEntity activitySkuEntity);
}
