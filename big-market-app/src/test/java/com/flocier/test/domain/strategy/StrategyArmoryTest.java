package com.flocier.test.domain.strategy;

import com.flocier.domain.strategy.service.armory.IStrategyArmory;
import com.flocier.domain.strategy.service.armory.IStrategyDisPatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner.class)
public class StrategyArmoryTest {
    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private IStrategyDisPatch strategyDisPath;

    @Test
    public void test_assembleLotteryStrategy(){
        strategyArmory.assembleLotteryStrategy(100001L);
    }
    @Test
    public void test_getRandomAwardId(){
        log.info("测试结果: {} -奖品值",strategyDisPath.getRandomAwardId(100001L));
        log.info("测试结果: {} -奖品值",strategyDisPath.getRandomAwardId(100001L));
        log.info("测试结果: {} -奖品值",strategyDisPath.getRandomAwardId(100001L));
        log.info("测试结果: {} -奖品值",strategyDisPath.getRandomAwardId(100001L));
    }
    @Test
    public void test_getRandomAwardIdWithRuleWeightValue(){
        log.info("测试结果: {} - 4000策略配置",strategyDisPath.getRandomAwardId(100001L,"4000:102,103,104,105"));
        log.info("测试结果: {} - 5000策略配置",strategyDisPath.getRandomAwardId(100001L,"5000:102,103,104,105,106,107"));
        log.info("测试结果: {} - 6000策略配置",strategyDisPath.getRandomAwardId(100001L,"6000:102,103,104,105,106,107,108,109"));
    }
}
