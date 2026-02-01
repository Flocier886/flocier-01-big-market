package com.flocier.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.flocier.domain.activity.model.entity.DeliveryOrderEntity;
import com.flocier.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.flocier.domain.credit.event.CreditAdjustSuccessMessageEvent;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.event.BaseEvent;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class CreditAdjustSuccessCustomer {
    @Value("${spring.rabbitmq.topic.credit_adjust_success}")
    private String topic;
    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    @RabbitListener(queuesToDeclare = @Queue(value = "${spring.rabbitmq.topic.credit_adjust_success}"))
    public void listener(String message){
        try{
            log.info("监听积分账户调整成功消息，进行交易商品发货 topic: {} message: {}", topic, message);
            //转换数据
            BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage>>() {
            }.getType());
            CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage creditAdjustSuccessMessage = eventMessage.getData();
            //积分发货
            DeliveryOrderEntity deliveryOrderEntity = new DeliveryOrderEntity();
            deliveryOrderEntity.setUserId(creditAdjustSuccessMessage.getUserId());
            deliveryOrderEntity.setOutBusinessNo(creditAdjustSuccessMessage.getOutBusinessNo());
            raffleActivityAccountQuotaService.updateOrder(deliveryOrderEntity);

        }catch (AppException e){
            if (ResponseCode.INDEX_DUP.getCode().equals(e.getCode())) {
                log.warn("监听积分账户调整成功消息，进行交易商品发货，消费重复 topic: {} message: {}", topic, message, e);
                return;
            }
            throw e;
        }catch (Exception e){
            log.error("监听积分账户调整成功消息，进行交易商品发货失败 topic: {} message: {}", topic, message, e);
            //TODO在MQ监听器中这种失败抛异常可能会导致MQ不断抛异常然后把消息重新发送一遍，然后就会导致不断的接受然后失败返回发送再接受，一直死循环
            throw e;
        }
    }
}
