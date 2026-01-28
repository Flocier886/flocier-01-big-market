package com.flocier.infrastructure.persistent.repository;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.alibaba.fastjson.JSON;
import com.flocier.domain.award.event.SendAwardMessageEvent;
import com.flocier.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.flocier.domain.award.model.entity.TaskEntity;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.infrastructure.event.EventPublisher;
import com.flocier.infrastructure.persistent.dao.ITaskDao;
import com.flocier.infrastructure.persistent.dao.IUserAwardRecordDao;
import com.flocier.infrastructure.persistent.po.Task;
import com.flocier.infrastructure.persistent.po.UserAwardRecord;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

@Slf4j
@Repository
public class AwardRepository implements IAwardRepository {
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private IUserAwardRecordDao userAwardRecordDao;
    @Resource
    private ITaskDao taskDao;
    @Resource
    private EventPublisher eventPublisher;

    @Override
    public void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate) {
        UserAwardRecordEntity userAwardRecordEntity=userAwardRecordAggregate.getUserAwardRecordEntity();
        TaskEntity taskEntity=userAwardRecordAggregate.getTaskEntity();
        String userId= userAwardRecordEntity.getUserId();
        Long activityId=userAwardRecordEntity.getActivityId();
        Integer awardId= userAwardRecordEntity.getAwardId();
        //构建流水对象
        UserAwardRecord userAwardRecord = new UserAwardRecord();
        userAwardRecord.setUserId(userAwardRecordEntity.getUserId());
        userAwardRecord.setActivityId(userAwardRecordEntity.getActivityId());
        userAwardRecord.setStrategyId(userAwardRecordEntity.getStrategyId());
        userAwardRecord.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecord.setAwardId(userAwardRecordEntity.getAwardId());
        userAwardRecord.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        userAwardRecord.setAwardTime(userAwardRecordEntity.getAwardTime());
        userAwardRecord.setAwardState(userAwardRecordEntity.getAwardState().getCode());
        //构建任务对象
        Task task = new Task();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setState(taskEntity.getState().getCode());
        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status->{
                try {
                    //写入记录
                    userAwardRecordDao.insert(userAwardRecord);
                    //写入任务
                    taskDao.insert(task);
                    return 1;
                }catch (DuplicateKeyException e){
                    status.setRollbackOnly();
                    log.error("写入中奖记录，唯一索引冲突 userId: {} activityId: {} awardId: {}", userId, activityId, awardId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        }finally {
            dbRouter.clear();
        }
        //发送MQ消息并修改task状态
        try {
            eventPublisher.publish(task.getTopic(),task.getMessage());
            taskDao.updateTaskSendMessageCompleted(task);
        }catch (Exception e){
            log.error("写入中奖记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
            taskDao.updateTaskSendMessageFail(task);
        }
    }
}
