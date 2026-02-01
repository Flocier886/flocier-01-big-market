package com.flocier.domain.activity.service.quota.policy.impl;

import com.flocier.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.flocier.domain.activity.model.vo.OrderStateVO;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service("credit_pay_trade")
public class CreditPayTradePolicy implements ITradePolicy {
    private final IActivityRepository activityRepository;

    public CreditPayTradePolicy(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        createQuotaOrderAggregate.setOrderState(OrderStateVO.wait_pay);
        activityRepository.doSaveCreditPayOrder(createQuotaOrderAggregate);

    }
}
