package com.flocier.infrastructure.persistent.repository;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.alibaba.fastjson.JSON;
import com.flocier.domain.award.event.SendAwardMessageEvent;
import com.flocier.domain.award.model.aggregate.GiveOutPrizesAggregate;
import com.flocier.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.flocier.domain.award.model.entity.TaskEntity;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;
import com.flocier.domain.award.model.entity.UserCreditAwardEntity;
import com.flocier.domain.award.model.vo.AccountStatusVO;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.infrastructure.event.EventPublisher;
import com.flocier.infrastructure.persistent.dao.*;
import com.flocier.infrastructure.persistent.po.Task;
import com.flocier.infrastructure.persistent.po.UserAwardRecord;
import com.flocier.infrastructure.persistent.po.UserCreditAccount;
import com.flocier.infrastructure.persistent.po.UserRaffleOrder;
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
    @Resource
    private IUserRaffleOrderDao userRaffleOrderDao;
    @Resource
    private IAwardDao awardDao;
    @Resource
    private IUserCreditAccountDao userCreditAccountDao;

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
        //构建更新抽奖单对象
        UserRaffleOrder userRaffleOrderReq = new UserRaffleOrder();
        userRaffleOrderReq.setUserId(userAwardRecordEntity.getUserId());
        userRaffleOrderReq.setOrderId(userAwardRecordEntity.getOrderId());

        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status->{
                try {
                    //写入记录
                    userAwardRecordDao.insert(userAwardRecord);
                    //写入任务
                    taskDao.insert(task);
                    //更新抽奖单
                    int count =userRaffleOrderDao.updateUserRaffleOrderStateUsed(userRaffleOrderReq);
                    if (count!=1) {
                        status.setRollbackOnly();
                        log.error("写入中奖记录，用户抽奖单已使用过，不可重复抽奖 userId: {} activityId: {} awardId: {}", userId, activityId, awardId);
                        throw new AppException(ResponseCode.ACTIVITY_ORDER_ERROR.getCode(), ResponseCode.ACTIVITY_ORDER_ERROR.getInfo());
                    }

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

    @Override
    public String queryAwardConfig(Integer awardId) {
        return awardDao.queryAwardConfigByAwardId(awardId);
    }

    @Override
    public void saveGiveOutPrizesAggregate(GiveOutPrizesAggregate giveOutPrizesAggregate) {
        String userId=giveOutPrizesAggregate.getUserId();
        UserCreditAwardEntity userCreditAwardEntity=giveOutPrizesAggregate.getUserCreditAwardEntity();
        UserAwardRecordEntity userAwardRecordEntity=giveOutPrizesAggregate.getUserAwardRecordEntity();
        //建立更新流水记录对象
        UserAwardRecord userAwardRecordReq = new UserAwardRecord();
        userAwardRecordReq.setUserId(userId);
        userAwardRecordReq.setOrderId(userAwardRecordEntity.getOrderId());
        userAwardRecordReq.setAwardState(userAwardRecordEntity.getAwardState().getCode());
        //建立更新用户积分对象
        UserCreditAccount userCreditAccountReq = new UserCreditAccount();
        userCreditAccountReq.setUserId(userCreditAwardEntity.getUserId());
        userCreditAccountReq.setTotalAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccountReq.setAvailableAmount(userCreditAwardEntity.getCreditAmount());
        userCreditAccountReq.setAccountStatus(AccountStatusVO.open.getCode());
        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
               try {
                   //更新用户积分账户
                   int updateAccountCount=userCreditAccountDao.updateAddAmount(userCreditAccountReq);
                   if(updateAccountCount==0){
                       userCreditAccountDao.insert(userCreditAccountReq);
                   }
                   //更新流水记录状态
                   int updateAwardRecordCount=userAwardRecordDao.updateAwardRecordCompletedState(userAwardRecordReq);
                   if(updateAwardRecordCount==0){
                       log.warn("更新中奖记录，重复更新拦截 userId:{} giveOutPrizesAggregate:{}", userId, JSON.toJSONString(giveOutPrizesAggregate));
                       status.setRollbackOnly();
                   }
                   return 1;
               }catch (DuplicateKeyException e){
                   status.setRollbackOnly();
                   log.error("更新中奖记录，唯一索引冲突 userId: {} ", userId, e);
                   throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
               }
            });
        }finally {
            dbRouter.clear();
        }

    }

    @Override
    public String queryAwardKey(Integer awardId) {
        return awardDao.queryAwardKeyByAwardId(awardId);
    }
}
