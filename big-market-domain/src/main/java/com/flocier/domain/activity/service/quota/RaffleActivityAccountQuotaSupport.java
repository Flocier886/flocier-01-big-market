package com.flocier.domain.activity.service.quota;

import com.flocier.domain.activity.model.entity.ActivityCountEntity;
import com.flocier.domain.activity.model.entity.ActivityEntity;
import com.flocier.domain.activity.model.entity.ActivitySkuEntity;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;

public class RaffleActivityAccountQuotaSupport {
    protected IActivityRepository repository;
    protected DefaultActivityChainFactory defaultActivityChainFactory;

    public RaffleActivityAccountQuotaSupport(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory) {
        this.repository = activityRepository;
        this.defaultActivityChainFactory = defaultActivityChainFactory;
    }

    ActivitySkuEntity queryActivitySku(Long sku){
        return repository.queryActivitySku(sku);
    }
    ActivityEntity queryRaffleActivityByActivityId(Long activityId){
        return repository.queryRaffleActivityByActivityId(activityId);
    }
    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId){
        return repository.queryRaffleActivityCountByActivityCountId(activityCountId);
    }

}
