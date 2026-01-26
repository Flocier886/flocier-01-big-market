package com.flocier.domain.activity.repository;

import com.flocier.domain.activity.model.entity.ActivityCountEntity;
import com.flocier.domain.activity.model.entity.ActivityEntity;
import com.flocier.domain.activity.model.entity.ActivitySkuEntity;

public interface IActivityRepository {
    ActivitySkuEntity queryActivitySku(Long sku);

    ActivityEntity queryRaffleActivityByActivityId(Long activityId);

    ActivityCountEntity queryRaffleActivityCountByActivityId(Long activityCountId);
}
