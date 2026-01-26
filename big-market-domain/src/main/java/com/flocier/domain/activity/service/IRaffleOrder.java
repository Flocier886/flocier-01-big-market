package com.flocier.domain.activity.service;

import com.flocier.domain.activity.model.entity.ActivityOrderEntity;
import com.flocier.domain.activity.model.entity.ActivityShopCartEntity;

public interface IRaffleOrder {
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);
}
