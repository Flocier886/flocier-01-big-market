package com.flocier.trigger.job;

import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.domain.task.model.entity.TaskEntity;
import com.flocier.domain.task.service.ITaskService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SendMessageTaskJob {
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private ITaskService taskService;
    @Resource
    private ThreadPoolExecutor executor;
    @Resource
    private RedissonClient redissonClient;

    @XxlJob("SendMessageTaskJob_DB1")
    public void exec_db01(){
        RLock lock=redissonClient.getLock("big-market-SendMessageTaskJob_DB1");
        boolean isLocked=false;
        try {
            isLocked=lock.tryLock(3,0, TimeUnit.SECONDS);
            if(!isLocked)return;
            //先路由
            dbRouter.setDBKey(1);
            dbRouter.setTBKey(0);
            //再查询相关数据
            List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
            if(taskEntities.isEmpty())return;
            //不为空，重新发送MQ消息
            for(TaskEntity task:taskEntities){
                //开启线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略，面试中容易对比提问。可以检索下相关资料。
                executor.execute(()->{
                    try {
                        log.info("定时任务，重新发送MQ消息 userId:{} topic:{}",task.getUserId(),task.getTopic());
                        taskService.sendMessage(task);
                        taskService.updateTaskSendMessageCompleted(task.getUserId(), task.getMessageId());
                    }catch (Exception e){
                        log.error("定时任务，发送MQ消息失败 userId: {} topic: {}", task.getUserId(), task.getTopic());
                        taskService.updateTaskSendMessageFail(task.getUserId(), task.getMessageId());
                    }
                });
            }
        }catch (Exception e){
            log.error("定时任务，扫描MQ任务表发送消息失败。", e);
        }finally {
            dbRouter.clear();
            if (isLocked){
                lock.unlock();
            }
        }
    }

    @XxlJob("SendMessageTaskJob_DB2")
    public void exec_db02(){
        RLock lock=redissonClient.getLock("big-market-SendMessageTaskJob_DB2");
        boolean isLocked=false;
        try {
            isLocked=lock.tryLock(3,0, TimeUnit.SECONDS);
            if(!isLocked)return;
            //先路由
            dbRouter.setDBKey(2);
            dbRouter.setTBKey(0);
            //再查询相关数据
            List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
            if(taskEntities.isEmpty())return;
            //不为空，重新发送MQ消息
            for(TaskEntity task:taskEntities){
                //开启线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略，面试中容易对比提问。可以检索下相关资料。
                executor.execute(()->{
                    try {
                        log.info("定时任务，重新发送MQ消息 userId:{} topic:{}",task.getUserId(),task.getTopic());
                        taskService.sendMessage(task);
                        taskService.updateTaskSendMessageCompleted(task.getUserId(), task.getMessageId());
                    }catch (Exception e){
                        log.error("定时任务，发送MQ消息失败 userId: {} topic: {}", task.getUserId(), task.getTopic());
                        taskService.updateTaskSendMessageFail(task.getUserId(), task.getMessageId());
                    }
                });
            }
        }catch (Exception e){
            log.error("定时任务，扫描MQ任务表发送消息失败。", e);
        }finally {
            dbRouter.clear();
            if (isLocked){
                lock.unlock();
            }
        }
    }
}
