package com.flocier.domain.activity.service;

import com.flocier.domain.activity.model.entity.ActivityOrderEntity;
import com.flocier.domain.activity.model.entity.ActivityShopCartEntity;
import com.flocier.domain.activity.model.entity.SkuRechargeEntity;

public interface IRaffleActivityAccountQuotaService {
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);

    String createOrder(SkuRechargeEntity skuRechargeEntity);
}
