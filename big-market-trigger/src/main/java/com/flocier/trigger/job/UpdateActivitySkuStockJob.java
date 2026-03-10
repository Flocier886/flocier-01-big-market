package com.flocier.trigger.job;

import com.flocier.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.flocier.domain.activity.service.IRaffleActivitySkuStockService;
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
public class UpdateActivitySkuStockJob {
    @Resource
    private IRaffleActivitySkuStockService skuStock;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ThreadPoolExecutor executor;

    @XxlJob("UpdateActivitySkuStockJob")
    public void exec(){
        RLock lock=redissonClient.getLock("big-market-UpdateActivitySkuStockJob");
        boolean isLocked=false;
        try {
            isLocked=lock.tryLock(3,0, TimeUnit.SECONDS);
            if(!isLocked)return;
            List<Long> skus=skuStock.querySkuList();
            for (Long sku:skus){
                executor.execute(()->{
                    ActivitySkuStockKeyVO activitySkuStockKeyVO=null;
                    try {
                        activitySkuStockKeyVO=skuStock.takeQueueValue(sku);
                    }catch (InterruptedException e){
                        log.error("定时任务，更新活动sku库存失败 sku: {}", sku);
                    }
                    if(activitySkuStockKeyVO==null)return;
                    log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
                    skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
                });
            }
        }catch (Exception e){
            log.error("定时任务，更新活动sku库存失败", e);
        }finally {
            if(isLocked){
                lock.unlock();
            }
        }
    }
}
