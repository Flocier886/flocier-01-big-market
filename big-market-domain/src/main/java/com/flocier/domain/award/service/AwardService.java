package com.flocier.domain.award.service;

import com.flocier.domain.award.event.SendAwardMessageEvent;
import com.flocier.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.flocier.domain.award.model.entity.TaskEntity;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;
import com.flocier.domain.award.model.vo.TaskStateVO;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.types.event.BaseEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AwardService implements IAwardService{
    @Resource
    private IAwardRepository awardRepository;
    @Resource
    private SendAwardMessageEvent sendAwardMessageEvent;
    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
        //保存订单流水和保存task任务与发送MQ消息
        //构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = SendAwardMessageEvent.SendAwardMessage.builder()
                .awardId(userAwardRecordEntity.getAwardId())
                .awardTitle(userAwardRecordEntity.getAwardTitle())
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
}
