package com.flocier.domain.award.service;

import com.flocier.domain.award.event.SendAwardMessageEvent;
import com.flocier.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.flocier.domain.award.model.entity.DistributeAwardEntity;
import com.flocier.domain.award.model.entity.TaskEntity;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;
import com.flocier.domain.award.model.vo.TaskStateVO;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.domain.award.service.distribute.IDistributeAward;
import com.flocier.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class AwardService implements IAwardService{
    private final IAwardRepository awardRepository;
    private final SendAwardMessageEvent sendAwardMessageEvent;
    private final Map<String, IDistributeAward> distributeAwardMap;

    public AwardService(IAwardRepository awardRepository, SendAwardMessageEvent sendAwardMessageEvent, Map<String, IDistributeAward> distributeAwardMap) {
        this.awardRepository = awardRepository;
        this.sendAwardMessageEvent = sendAwardMessageEvent;
        this.distributeAwardMap = distributeAwardMap;
    }

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
        //保存订单流水和保存task任务与发送MQ消息
        //构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = SendAwardMessageEvent.SendAwardMessage.builder()
                .awardId(userAwardRecordEntity.getAwardId())
                .awardTitle(userAwardRecordEntity.getAwardTitle())
                .orderId(userAwardRecordEntity.getOrderId())
                .awardConfig(userAwardRecordEntity.getAwardConfig())
                .userId(userAwardRecordEntity.getUserId())
                .build();
        BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> sendAwardMessageEventMessage = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);
        //构建任务对象
        TaskEntity taskEntity = TaskEntity.builder()
                .userId(userAwardRecordEntity.getUserId())
                .state(TaskStateVO.create)
                .topic(sendAwardMessageEvent.topic())
                .message(sendAwardMessageEventMessage)
                .messageId(sendAwardMessageEventMessage.getId())
                .build();

        //构建聚合对象
        UserAwardRecordAggregate userAwardRecordAggregate=UserAwardRecordAggregate.builder()
                .userAwardRecordEntity(userAwardRecordEntity)
                .taskEntity(taskEntity)
                .build();
        awardRepository.saveUserAwardRecord(userAwardRecordAggregate);
    }

    @Override
    public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
        //查询奖品的key,然后匹配对应的发放奖品的方法
        String awardKey = awardRepository.queryAwardKey(distributeAwardEntity.getAwardId());
        if (null == awardKey) {
            log.error("分发奖品，奖品ID不存在。awardKey:{}", awardKey);
            return;
        }
        IDistributeAward distributeAward=distributeAwardMap.get(awardKey);
        if (null == distributeAward) {
            //TODO 完善后续的奖品总类发放
            log.error("分发奖品，对应的服务不存在。awardKey:{}", awardKey);
            throw new RuntimeException("分发奖品，奖品" + awardKey + "对应的服务不存在");
        }
        // 发放奖品
        distributeAward.giveOutPrizes(distributeAwardEntity);
    }
}
