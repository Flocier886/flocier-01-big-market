package com.flocier.domain.activity.service.quota.rule.impl;

import com.flocier.domain.activity.model.entity.ActivityCountEntity;
import com.flocier.domain.activity.model.entity.ActivityEntity;
import com.flocier.domain.activity.model.entity.ActivitySkuEntity;
import com.flocier.domain.activity.model.vo.ActivityStateVO;
import com.flocier.domain.activity.service.quota.rule.AbstractActionChain;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component("activity_base_action")
public class ActivityBaseActionChain extends AbstractActionChain {
    @Override
    public boolean action(ActivityEntity activityEntity, ActivityCountEntity activityCountEntity, ActivitySkuEntity activitySkuEntity) {
        log.info("活动责任链-基础信息【有效期、状态、库存(sku)】校验开始。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
        //检查活动状态
        if(!ActivityStateVO.open.equals(activityEntity.getState())){
            log.info("状态： {}",activityEntity.getState());
            throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(), ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
        }
        //检查活动是否过期
        Date now=new Date();
        if (activityEntity.getBeginDateTime().after(now) || activityEntity.getEndDateTime().before(now)){
            throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(), ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
        }
        //检查是否还有库存
        if(activitySkuEntity.getStockCountSurplus()<=0){
            throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
        }
        //放行
        return next().action(activityEntity,activityCountEntity,activitySkuEntity);

    }
}
