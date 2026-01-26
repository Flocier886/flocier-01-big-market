package com.flocier.domain.activity.service;

import com.alibaba.fastjson.JSON;
import com.flocier.domain.activity.model.aggregrate.CreateOrderAggregate;
import com.flocier.domain.activity.model.entity.*;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.activity.service.rule.IActionChain;
import com.flocier.domain.activity.service.rule.factory.DefaultActivityChainFactory;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractRaffleActivity extends RaffleActivitySupport implements IRaffleOrder{

    public AbstractRaffleActivity(IActivityRepository activityRepository, DefaultActivityChainFactory defaultActivityChainFactory) {
        super(activityRepository, defaultActivityChainFactory);
    }

    @Override
    public ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity) {
        //通过sku查询相关活动sku信息
        //TODO这步过后应该还有空指针判断校验
        ActivitySkuEntity activitySkuEntity=queryActivitySku(activityShopCartEntity.getSku());
        //查询活动信息
        ActivityEntity activityEntity=queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        //查询次数信息
        ActivityCountEntity activityCountEntity=queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        //生成日志
        log.info("查询结果：{} {} {}", JSON.toJSONString(activitySkuEntity), JSON.toJSONString(activityEntity), JSON.toJSONString(activityCountEntity));
        return ActivityOrderEntity.builder().build();
    }
    @Override
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity){
        //参数校验
        String userId=skuRechargeEntity.getUserId();
        Long sku=skuRechargeEntity.getSku();
        String outBusinessNo=skuRechargeEntity.getOutBusinessNo();
        if(StringUtils.isBlank(userId) || sku==null || StringUtils.isBlank(outBusinessNo)){
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        //查询相关信息
        //通过sku查询相关活动sku信息
        ActivitySkuEntity activitySkuEntity=queryActivitySku(sku);
        //查询活动信息
        ActivityEntity activityEntity=queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        //查询次数信息
        ActivityCountEntity activityCountEntity=queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        //活动规则校验
        IActionChain actionChain= defaultActivityChainFactory.openActionChain();
        boolean success=actionChain.action(activityEntity,activityCountEntity,activitySkuEntity);
        //创建订单聚合对象
        CreateOrderAggregate createOrderAggregate=buildOrderAggregate(skuRechargeEntity,activitySkuEntity,activityEntity,activityCountEntity);
        //保存订单
        doSaveOrder(createOrderAggregate);
        //返回单号
        return createOrderAggregate.getActivityOrderEntity().getOrderId();
    }

    protected abstract void doSaveOrder(CreateOrderAggregate createOrderAggregate);

    protected abstract CreateOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);
}
