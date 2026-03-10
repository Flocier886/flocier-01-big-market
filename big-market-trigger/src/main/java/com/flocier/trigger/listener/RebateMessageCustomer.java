package com.flocier.trigger.listener;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.flocier.domain.activity.model.entity.SkuRechargeEntity;
import com.flocier.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.flocier.domain.credit.model.entity.TradeEntity;
import com.flocier.domain.credit.model.vo.TradeNameVO;
import com.flocier.domain.credit.model.vo.TradeTypeVO;
import com.flocier.domain.credit.service.ICreditAdjustService;
import com.flocier.domain.rebate.event.SendRebateMessageEvent;
import com.flocier.domain.rebate.model.vo.RebateTypeVO;
import com.flocier.domain.rebate.service.IBehaviorRebateService;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.event.BaseEvent;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

@Slf4j
@Component
public class RebateMessageCustomer {
    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String topic;
    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    @Resource
    private ICreditAdjustService creditAdjustService;


    @RabbitListener(queuesToDeclare = @Queue(value = "${spring.rabbitmq.topic.send_rebate}"))
    public void listener(String message){
        try {
            log.info("监听用户行为返利消息 topic: {} message: {}", topic, message);
            //转换消息
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage>eventMessage= JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage>>() {
            }.getType());
            SendRebateMessageEvent.RebateMessage rebateMessage=eventMessage.getData();
            switch (rebateMessage.getRebateType()){
                case "sku":
                    SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
                    skuRechargeEntity.setUserId(rebateMessage.getUserId());
                    skuRechargeEntity.setSku(Long.valueOf(rebateMessage.getRebateConfig()));
                    skuRechargeEntity.setOutBusinessNo(rebateMessage.getBizId());
                    raffleActivityAccountQuotaService.createOrder(skuRechargeEntity);
                    break;
                case "integral":
                    TradeEntity tradeEntity=TradeEntity.builder()
                            .userId(rebateMessage.getUserId())
                            .tradeName(TradeNameVO.REBATE)
                            .tradeType(TradeTypeVO.FORWARD)
                            .amount(new BigDecimal(rebateMessage.getRebateConfig()))
                            .outBusinessNo(rebateMessage.getBizId())
                            .build();
                    creditAdjustService.createOrder(tradeEntity);
                    break;
            }

        }catch (AppException e){
            if (ResponseCode.INDEX_DUP.getCode().equals(e.getCode())) {
                log.warn("监听用户行为返利消息，消费重复 topic: {} message: {}", topic, message, e);
                return;
            }
            if (ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode().equals(e.getCode())) {
                log.warn("监听用户行为返利消息，活动库存不足 topic: {} message: {}", topic, message, e);
                return;
            }
            throw e;
        }catch (Exception e){
            log.error("监听用户行为返利消息，消费失败 topic: {} message: {}", topic, message, e);
            throw e;
        }
    }
}
