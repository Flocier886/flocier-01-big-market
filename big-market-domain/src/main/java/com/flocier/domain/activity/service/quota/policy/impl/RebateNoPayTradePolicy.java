package com.flocier.domain.activity.service.quota.policy.impl;

import com.flocier.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.flocier.domain.activity.model.vo.OrderStateVO;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service("rebate_no_pay_trade")
public class RebateNoPayTradePolicy implements ITradePolicy {
    private final IActivityRepository activityRepository;

    public RebateNoPayTradePolicy(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }


    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        // 不需要支付则修改订单金额为0，状态为完成，直接给用户账户充值
        createQuotaOrderAggregate.setOrderState(OrderStateVO.completed);
        createQuotaOrderAggregate.getActivityOrderEntity().setPayAmount(BigDecimal.ZERO);
        activityRepository.doSaveNoPayOrder(createQuotaOrderAggregate);
    }
}
