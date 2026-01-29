package com.flocier.domain.activity.service.armory;

import com.flocier.domain.activity.model.entity.ActivityCountEntity;
import com.flocier.domain.activity.model.entity.ActivityEntity;
import com.flocier.domain.activity.model.entity.ActivitySkuEntity;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.strategy.model.entity.StrategyRuleEntity;
import com.flocier.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ActivityArmory implements IActivityArmory,IActivityDispatch{
    @Resource
    private IActivityRepository activityRepository;
    @Override
    public boolean assembleActivitySku(Long sku) {
        //查询sku库存
        ActivitySkuEntity activitySkuEntity=activityRepository.queryActivitySku(sku);
        //缓存到redis
        cacheActivitySkuStockCount(sku,activitySkuEntity.getStockCount());
        //预热活动与活动计数
        ActivityEntity activityEntity = activityRepository.queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        ActivityCountEntity activityCountEntity = activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        return true;
    }

    @Override
    public void assembleActivitySkuByActivityId(Long activityId) {
        //查询当前活动的所有sku
        List<ActivitySkuEntity> activitySkuEntities=activityRepository.queryActivitySkuListByActivityId(activityId);
        //缓存所有sku到redis
        for (ActivitySkuEntity activitySkuEntity:activitySkuEntities){
            cacheActivitySkuStockCount(activitySkuEntity.getSku(),activitySkuEntity.getStockCount());
            //预热活动对应sku次数
            activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        }
        //预热活动
        activityRepository.queryRaffleActivityByActivityId(activityId);
    }

    private void cacheActivitySkuStockCount(Long sku, Integer stockCount) {
        String cacheKey= Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY+sku;
        activityRepository.cacheActivitySkuStockCount(cacheKey,stockCount);
    }

    @Override
    public boolean subtractionActivitySkuStock(Long sku, Date endDateTime) {
        String cacheKey=Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY+sku;
        return activityRepository.subtractionActivitySkuStock(sku,cacheKey,endDateTime);
    }
}
