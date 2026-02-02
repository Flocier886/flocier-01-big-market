package com.flocier.domain.credit.repository;

import com.flocier.domain.credit.model.aggregate.TradeAggregate;
import com.flocier.domain.credit.model.entity.CreditAccountEntity;

public interface ICreditRepository {
    void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);

    CreditAccountEntity queryUserCreditAccount(String userId);
}
