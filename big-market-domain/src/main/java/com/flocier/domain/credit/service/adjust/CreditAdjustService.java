package com.flocier.domain.credit.service.adjust;

import com.flocier.domain.credit.event.CreditAdjustSuccessMessageEvent;
import com.flocier.domain.credit.model.aggregate.TradeAggregate;
import com.flocier.domain.credit.model.entity.CreditAccountEntity;
import com.flocier.domain.credit.model.entity.CreditOrderEntity;
import com.flocier.domain.credit.model.entity.TaskEntity;
import com.flocier.domain.credit.model.entity.TradeEntity;
import com.flocier.domain.credit.repository.ICreditRepository;
import com.flocier.domain.credit.service.ICreditAdjustService;
import com.flocier.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class CreditAdjustService implements ICreditAdjustService {
    @Resource
    private ICreditRepository creditRepository;
    @Resource
    private CreditAdjustSuccessMessageEvent creditAdjustSuccessMessageEvent;

    @Override
    public String createOrder(TradeEntity tradeEntity) {
        log.info("修改账户积分额度开始 userId:{} tradeName:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getAmount());
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
        //构建消息对象
        CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage creditAdjustSuccessMessage = new CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage();
        creditAdjustSuccessMessage.setUserId(tradeEntity.getUserId());
        creditAdjustSuccessMessage.setOrderId(creditOrderEntity.getOrderId());
        creditAdjustSuccessMessage.setAmount(tradeEntity.getAmount());
        creditAdjustSuccessMessage.setOutBusinessNo(tradeEntity.getOutBusinessNo());
        BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> creditAdjustSuccessMessageEventMessage = creditAdjustSuccessMessageEvent.buildEventMessage(creditAdjustSuccessMessage);
        //填充任务对象
        TaskEntity taskEntity = TradeAggregate.createTaskEntity(tradeEntity.getUserId(), creditAdjustSuccessMessageEvent.topic(), creditAdjustSuccessMessageEventMessage.getId(), creditAdjustSuccessMessageEventMessage);
        //创建聚合对象
        TradeAggregate tradeAggregate=new TradeAggregate();
        tradeAggregate.setUserId(tradeEntity.getUserId());
        tradeAggregate.setCreditOrderEntity(creditOrderEntity);
        tradeAggregate.setCreditAccountEntity(creditAccountEntity);
        tradeAggregate.setTaskEntity(taskEntity);
        //保存订单并修改账户
        creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        log.info("增加账户积分额度完成 userId:{} orderId:{}", tradeEntity.getUserId(), creditOrderEntity.getOrderId());

        return creditOrderEntity.getOrderId();
    }

    @Override
    public CreditAccountEntity queryUserCreditAccount(String userId) {
        return creditRepository.queryUserCreditAccount(userId);
    }
}
