package com.flocier.trigger.job;

import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.domain.task.model.entity.TaskEntity;
import com.flocier.domain.task.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class SendMessageTaskJob {
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private ITaskService taskService;
    @Resource
    private ThreadPoolExecutor executor;


    @Scheduled(cron = "0/5 * * * * ?")
    public void exec(){
        try {
            //获取分库数量
            int dbCount=dbRouter.dbCount();
            //逐库扫描
            for (int dbIdx=1;dbIdx<=dbCount;dbIdx++){
                int finalDbIdx=dbIdx;
                //开启线程池
                executor.execute(()->{
                    try {
                        //先路由
                        dbRouter.setDBKey(finalDbIdx);
                        dbRouter.setTBKey(0);
                        //再查询相关数据
                        List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
                        if(taskEntities.isEmpty())return;
                        //不为空，重新发送MQ消息
                        for(TaskEntity task:taskEntities){
                            //开启线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略，面试中容易对比提问。可以检索下相关资料。
                            executor.execute(()->{
                                try {
                                    taskService.sendMessage(task);
                                    taskService.updateTaskSendMessageCompleted(task.getUserId(), task.getMessageId());
                                }catch (Exception e){
                                    log.error("定时任务，发送MQ消息失败 userId: {} topic: {}", task.getUserId(), task.getTopic());
                                    taskService.updateTaskSendMessageFail(task.getUserId(), task.getMessageId());
                                }
                            });
                        }
                    }finally {
                        dbRouter.clear();
                    }
                });
            }
        }catch (Exception e){
            log.error("定时任务，扫描MQ任务表发送消息失败。", e);
        }finally {
            dbRouter.clear();
        }
    }
}
