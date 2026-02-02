package com.flocier.domain.credit.service;

import com.flocier.domain.credit.model.entity.CreditAccountEntity;
import com.flocier.domain.credit.model.entity.TradeEntity;

public interface ICreditAdjustService {
    /**
     * 创建增加积分额度订单
     * @param tradeEntity 交易实体对象
     * @return 单号
     */
    String createOrder(TradeEntity tradeEntity);

    CreditAccountEntity queryUserCreditAccount(String userId);
}
