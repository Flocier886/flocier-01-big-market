package com.flocier.domain.credit.service;

import com.flocier.domain.credit.model.aggregate.TradeAggregate;
import com.flocier.domain.credit.model.entity.CreditAccountEntity;
import com.flocier.domain.credit.model.entity.CreditOrderEntity;
import com.flocier.domain.credit.model.entity.TradeEntity;
import com.flocier.domain.credit.repository.ICreditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class CreditAdjustService implements ICreditAdjustService{
    @Resource
    private ICreditRepository creditRepository;

    @Override
    public String createOrder(TradeEntity tradeEntity) {
        log.info("增加账户积分额度开始 userId:{} tradeName:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getAmount());
        //创建积分账户修改实体
        CreditAccountEntity creditAccountEntity = TradeAggregate.createCreditAccountEntity(tradeEntity.getUserId(), tradeEntity.getAmount());
        //创建交易订单实体
        CreditOrderEntity creditOrderEntity = TradeAggregate.createCreditOrderEntity(
                tradeEntity.getUserId(),
                tradeEntity.getTradeName(),
                tradeEntity.getTradeType(),
                tradeEntity.getAmount(),
                tradeEntity.getOutBusinessNo()
        );
        //创建聚合对象
        TradeAggregate tradeAggregate=new TradeAggregate();
        tradeAggregate.setUserId(tradeEntity.getUserId());
        tradeAggregate.setCreditOrderEntity(creditOrderEntity);
        tradeAggregate.setCreditAccountEntity(creditAccountEntity);
        //保存订单并修改账户
        creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        log.info("增加账户积分额度完成 userId:{} orderId:{}", tradeEntity.getUserId(), creditOrderEntity.getOrderId());

        return creditOrderEntity.getOrderId();
    }
}
