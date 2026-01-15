package com.flocier.test.domain;

import com.flocier.domain.strategy.service.armory.IStrategyArmory;
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

    @Test
    public void test_assembleLotteryStrategy(){
        strategyArmory.assembleLotteryStrategy(100001L);
    }
    @Test
    public void test_getRandomAwardId(){
        log.info("测试结果: {} -奖品值",strategyArmory.getRandomAwardId(100001L));
        log.info("测试结果: {} -奖品值",strategyArmory.getRandomAwardId(100001L));
        log.info("测试结果: {} -奖品值",strategyArmory.getRandomAwardId(100001L));
        log.info("测试结果: {} -奖品值",strategyArmory.getRandomAwardId(100001L));
    }
}
