package com.flocier.domain.rebate.service;

import com.flocier.domain.award.model.vo.TaskStateVO;
import com.flocier.domain.rebate.event.SendRebateMessageEvent;
import com.flocier.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import com.flocier.domain.rebate.model.entity.BehaviorEntity;
import com.flocier.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import com.flocier.domain.rebate.model.entity.TaskEntity;
import com.flocier.domain.rebate.model.vo.DailyBehaviorRebateVO;
import com.flocier.domain.rebate.repository.IBehaviorRebateRepository;
import com.flocier.types.common.Constants;
import com.flocier.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class BehaviorRebateService implements IBehaviorRebateService {
    @Resource
    private IBehaviorRebateRepository repository;
    @Resource
    private SendRebateMessageEvent sendRebateMessageEvent;
    @Override
    public List<String> createOrder(BehaviorEntity behaviorEntity) {
        //查询返利配置
        List<DailyBehaviorRebateVO> dailyBehaviorRebateVOS=repository.queryDailyBehaviorRebateConfig(behaviorEntity.getBehaviorTypeVO());
        if(dailyBehaviorRebateVOS==null || dailyBehaviorRebateVOS.isEmpty())return new ArrayList<>();
        //构建聚合对象
        List<String> orderIds=new ArrayList<>();
        List<BehaviorRebateAggregate> behaviorRebateAggregates=new ArrayList<>();
        for(DailyBehaviorRebateVO dailyBehaviorRebateVO:dailyBehaviorRebateVOS){
            String bizId=behaviorEntity.getUserId()+ Constants.UNDERLINE+dailyBehaviorRebateVO.getRebateType()+Constants.UNDERLINE+behaviorEntity.getOutBusinessNo();
            BehaviorRebateOrderEntity behaviorRebateOrderEntity = BehaviorRebateOrderEntity.builder()
                    .userId(behaviorEntity.getUserId())
                    .orderId(RandomStringUtils.randomNumeric(12))
                    .behaviorType(dailyBehaviorRebateVO.getBehaviorType())
                    .rebateDesc(dailyBehaviorRebateVO.getRebateDesc())
                    .rebateType(dailyBehaviorRebateVO.getRebateType())
                    .rebateConfig(dailyBehaviorRebateVO.getRebateConfig())
                    .bizId(bizId)
                    .build();
            orderIds.add(behaviorRebateOrderEntity.getOrderId());
            //构建消息体对象
            SendRebateMessageEvent.RebateMessage rebateMessage= SendRebateMessageEvent.RebateMessage.builder()
                    .userId(behaviorEntity.getUserId())
                    .rebateType(dailyBehaviorRebateVO.getBehaviorType())
                    .rebateConfig(dailyBehaviorRebateVO.getRebateConfig())
                    .bizId(bizId)
                    .build();
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> rebateMessageEventMessage = sendRebateMessageEvent.buildEventMessage(rebateMessage);
            //构建任务对象
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUserId(behaviorEntity.getUserId());
            taskEntity.setTopic(sendRebateMessageEvent.topic());
            taskEntity.setMessageId(rebateMessageEventMessage.getId());
            taskEntity.setMessage(rebateMessageEventMessage);
            taskEntity.setState(TaskStateVO.create);
            //拼装聚合对象
            BehaviorRebateAggregate behaviorRebateAggregate = BehaviorRebateAggregate.builder()
                    .userId(behaviorEntity.getUserId())
                    .behaviorRebateOrderEntity(behaviorRebateOrderEntity)
                    .taskEntity(taskEntity)
                    .build();
            behaviorRebateAggregates.add(behaviorRebateAggregate);
        }
        //实现订单保存与MQ消息发送
        repository.saveUserRebateRecord(behaviorEntity.getUserId(), behaviorRebateAggregates);
        return orderIds;
    }
}
