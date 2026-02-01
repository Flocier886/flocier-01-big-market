package com.flocier.domain.activity.service.quota.policy;

import com.flocier.domain.activity.model.aggregate.CreateQuotaOrderAggregate;

public interface ITradePolicy {
    void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate);
}
