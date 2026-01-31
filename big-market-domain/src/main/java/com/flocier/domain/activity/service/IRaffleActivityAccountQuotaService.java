package com.flocier.domain.activity.service;

import com.flocier.domain.activity.model.entity.ActivityAccountEntity;
import com.flocier.domain.activity.model.entity.ActivityOrderEntity;
import com.flocier.domain.activity.model.entity.ActivityShopCartEntity;
import com.flocier.domain.activity.model.entity.SkuRechargeEntity;

public interface IRaffleActivityAccountQuotaService {
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);

    String createOrder(SkuRechargeEntity skuRechargeEntity);

    Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId);

    ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId);

    Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);
}
