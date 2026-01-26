package com.flocier.domain.activity.service;

import com.alibaba.fastjson.JSON;
import com.flocier.domain.activity.model.entity.*;
import com.flocier.domain.activity.repository.IActivityRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRaffleActivity implements IRaffleOrder{
    protected IActivityRepository repository;

    public AbstractRaffleActivity(IActivityRepository activityRepository) {
        this.repository = activityRepository;
    }
    @Override
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity) {
        //通过sku查询相关活动sku信息
        ActivitySkuEntity activitySkuEntity=repository.queryActivitySku(activityShopCartEntity.getSku());
        //查询活动信息
        ActivityEntity activityEntity=repository.queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        //查询次数信息
        ActivityCountEntity activityCountEntity=repository.queryRaffleActivityCountByActivityId(activitySkuEntity.getActivityCountId());
        //生成日志
        log.info("查询结果：{} {} {}", JSON.toJSONString(activitySkuEntity), JSON.toJSONString(activityEntity), JSON.toJSONString(activityCountEntity));
        return ActivityOrderEntity.builder().build();
    }
}
