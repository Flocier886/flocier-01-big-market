package com.flocier.domain.activity.service;

import com.flocier.domain.activity.model.entity.ActivityOrderEntity;
import com.flocier.domain.activity.model.entity.ActivityShopCartEntity;
import com.flocier.domain.activity.model.entity.SkuRechargeEntity;

public interface IRaffleOrder {
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);

    String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity);
}
