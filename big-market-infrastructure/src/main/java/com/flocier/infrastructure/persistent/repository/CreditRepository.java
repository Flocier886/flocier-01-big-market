package com.flocier.infrastructure.persistent.repository;

import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.alibaba.fastjson.JSON;
import com.flocier.domain.credit.model.aggregate.TradeAggregate;
import com.flocier.domain.credit.model.entity.CreditAccountEntity;
import com.flocier.domain.credit.model.entity.CreditOrderEntity;
import com.flocier.domain.credit.model.entity.TaskEntity;
import com.flocier.domain.credit.model.vo.AccountStatusVO;
import com.flocier.domain.credit.repository.ICreditRepository;
import com.flocier.infrastructure.event.EventPublisher;
import com.flocier.infrastructure.persistent.dao.ITaskDao;
import com.flocier.infrastructure.persistent.dao.IUserCreditAccountDao;
import com.flocier.infrastructure.persistent.dao.IUserCreditOrderDao;
import com.flocier.infrastructure.persistent.po.Task;
import com.flocier.infrastructure.persistent.po.UserCreditAccount;
import com.flocier.infrastructure.persistent.po.UserCreditOrder;
import com.flocier.infrastructure.persistent.redis.IRedisService;
import com.flocier.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class CreditRepository implements ICreditRepository {
    @Resource
    private IRedisService redisService;
    @Resource
    private IUserCreditOrderDao userCreditOrderDao;
    @Resource
    private IUserCreditAccountDao userCreditAccountDao;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private ITaskDao taskDao;
    @Resource
    private EventPublisher eventPublisher;
    @Override
    public void saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
        String userId=tradeAggregate.getUserId();
        CreditAccountEntity creditAccountEntity=tradeAggregate.getCreditAccountEntity();
        CreditOrderEntity creditOrderEntity=tradeAggregate.getCreditOrderEntity();
        TaskEntity taskEntity=tradeAggregate.getTaskEntity();
        //创建积分账户
        UserCreditAccount userCreditAccountReq=new UserCreditAccount();
        userCreditAccountReq.setUserId(userId);
        userCreditAccountReq.setTotalAmount(creditAccountEntity.getAdjustAmount());
        userCreditAccountReq.setAvailableAmount(creditAccountEntity.getAdjustAmount());
        //创建订单
        UserCreditOrder userCreditOrderReq = new UserCreditOrder();
        userCreditOrderReq.setUserId(creditOrderEntity.getUserId());
        userCreditOrderReq.setOrderId(creditOrderEntity.getOrderId());
        userCreditOrderReq.setTradeName(creditOrderEntity.getTradeName().getName());
        userCreditOrderReq.setTradeType(creditOrderEntity.getTradeType().getCode());
        userCreditOrderReq.setTradeAmount(creditOrderEntity.getTradeAmount());
        userCreditOrderReq.setOutBusinessNo(creditOrderEntity.getOutBusinessNo());
        //建立任务
        Task task = new Task();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
        task.setState(taskEntity.getState().getCode());

        //上锁，提高代码可用性
        RLock lock = redisService.getLock(Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK + userId + Constants.UNDERLINE + creditOrderEntity.getOutBusinessNo());
        try{
            lock.lock(3, TimeUnit.SECONDS);
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    //更新账户积分
                    UserCreditAccount userCreditAccount = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
                    if (null == userCreditAccount) {
                        //TODO这里数据库的open字段好像并没有起到任何过滤屏蔽作用，后期应该修改对应的mapper的数据库语句
                        userCreditAccountReq.setAccountStatus(AccountStatusVO.open.getCode());
                        userCreditAccountDao.insert(userCreditAccountReq);
                    } else {
                        userCreditAccountDao.updateAddAmount(userCreditAccountReq);
                    }
                    //保存订单
                    userCreditOrderDao.insert(userCreditOrderReq);
                    //写入任务
                    taskDao.insert(task);
                }catch (DuplicateKeyException e){
                    //TODO注意这里没有再次抛出异常而是选择吸收异常
                    status.setRollbackOnly();
                    log.error("调整账户积分额度异常，唯一索引冲突 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
                }catch (Exception e){
                    status.setRollbackOnly();
                    log.error("调整账户积分额度失败 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
                }
                return 1;
            });
        }finally {
            dbRouter.clear();
            lock.unlock();
        }
        try {
            // 发送消息【在事务外执行，如果失败还有任务补偿】
            eventPublisher.publish(task.getTopic(), task.getMessage());
            // 更新数据库记录，task 任务表
            taskDao.updateTaskSendMessageCompleted(task);
            log.info("调整账户积分记录，发送MQ消息完成 userId: {} orderId:{} topic: {}", userId, creditOrderEntity.getOrderId(), task.getTopic());
        } catch (Exception e) {
            log.error("调整账户积分记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
            taskDao.updateTaskSendMessageFail(task);
        }

    }

    @Override
    public CreditAccountEntity queryUserCreditAccount(String userId) {
        UserCreditAccount userCreditAccountReq=new UserCreditAccount();
        userCreditAccountReq.setUserId(userId);
        try {
            dbRouter.doRouter(userId);
            UserCreditAccount userCreditAccount = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
            return CreditAccountEntity.builder().userId(userId).adjustAmount(userCreditAccount.getAvailableAmount()).build();
        }finally {
            dbRouter.clear();
        }
    }
}
