package com.flocier.trigger.job;

import com.flocier.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.flocier.domain.strategy.repository.IStrategyRepository;
import com.flocier.domain.strategy.service.IRaffleAward;
import com.flocier.domain.strategy.service.IRaffleStock;
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
public class UpdateAwardStockJob {
    @Resource
    private IRaffleStock raffleStock;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ThreadPoolExecutor executor;
    @Resource
    private IRaffleAward raffleAward;

    @XxlJob("UpdateAwardStockJob")
    public void exec(){
        RLock lock=redissonClient.getLock("big-market-UpdateAwardStockJob");
        boolean isLocked=false;
        try {
            isLocked=lock.tryLock(3,0, TimeUnit.SECONDS);
            if(!isLocked)return;

            List<StrategyAwardStockKeyVO> strategyAwardStockKeyVOS=raffleAward.queryOpenActivityStrategyAwardList();
            if(strategyAwardStockKeyVOS==null)return;
            for (StrategyAwardStockKeyVO strategyAwardStockKeyVO:strategyAwardStockKeyVOS){
                executor.execute(()->{
                    try {
                        StrategyAwardStockKeyVO queueStrategyAwardStockKeyVO=raffleStock.takeQueueValue(strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
                        if (queueStrategyAwardStockKeyVO==null)return;
                        log.info("定时任务，更新奖品消耗库存 strategyId:{} awardId:{}", queueStrategyAwardStockKeyVO.getStrategyId(), queueStrategyAwardStockKeyVO.getAwardId());
                        raffleStock.updateStrategyAwardStock(queueStrategyAwardStockKeyVO.getStrategyId(), queueStrategyAwardStockKeyVO.getAwardId());
                    }catch (InterruptedException e){
                        log.error("定时任务，更新奖品消耗库存失败 strategyId:{} awardId:{}", strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
                    }
                });
            }
        }catch (Exception e){
            log.error("定时任务，更新奖品消耗库存失败", e);
        }finally {
            if (isLocked){
                lock.unlock();
            }
        }
    }
}
