package com.flocier.test.domain;

import com.alibaba.fastjson.JSON;
import com.flocier.domain.strategy.model.entity.RaffleAwardEntity;
import com.flocier.domain.strategy.model.entity.RaffleFactorEntity;
import com.flocier.domain.strategy.service.IRaffleStrategy;
import com.flocier.domain.strategy.service.armory.IStrategyArmory;
import com.flocier.domain.strategy.service.rule.chain.ILogicChain;
import com.flocier.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.flocier.domain.strategy.service.rule.chain.impl.DefaultLogicChain;
import com.flocier.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import com.flocier.domain.strategy.service.rule.filter.impl.RuleLockLogicFilter;
import com.flocier.domain.strategy.service.rule.filter.impl.RuleWeightLogicFilter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.internal.bytebuddy.implementation.ToStringMethod;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner.class)
public class RaffleStrategyTest {
    @Resource
    private RuleWeightLogicFilter ruleWeightLogicFilter;
    @Resource
    private RuleLockLogicFilter ruleLockLogicFilter;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private DefaultChainFactory defaultChainFactory;
    @Before
    public void setUp() {
        //log.info("3号抽奖初始化测试结果： {}",strategyArmory.assembleLotteryStrategy(100003L));
        ReflectionTestUtils.setField(ruleWeightLogicFilter, "userScore", 5500L);
        ReflectionTestUtils.setField(ruleLockLogicFilter, "userRaffleCount", 0L);

    }

    @Test
    public void test_performRaffle() {
        RaffleFactorEntity raffleFactorEntity = RaffleFactorEntity.builder()
                .userId("flocier")
                .strategyId(100001L)
                .build();
        RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(raffleFactorEntity);
        log.info("请求参数：{}", JSON.toJSONString(raffleFactorEntity));
        log.info("测试结果：{}", JSON.toJSONString(raffleAwardEntity));
    }
    @Test
    public void test_rule_center_lock_rule() {
        RaffleFactorEntity raffleFactorEntity = RaffleFactorEntity.builder()
                .userId("flocier")
                .strategyId(100003L)
                .build();
        RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(raffleFactorEntity);
        log.info("请求参数：{}", JSON.toJSONString(raffleFactorEntity));
        log.info("测试结果：{}", JSON.toJSONString(raffleAwardEntity));
    }
    @Test
    public void test_LogicChain_rule_blacklist() {
        ILogicChain logicChain = defaultChainFactory.openLogicChain(100001L);
        Integer awardId = logicChain.logic("user001", 100001L);
        log.info("测试结果：{}", awardId);
    }
    @Test
    public void test_LogicChain_rule_default() {
        ILogicChain logicChain = defaultChainFactory.openLogicChain(100001L);
        Integer awardId = logicChain.logic("flocier", 100001L);
        log.info("测试结果：{}", awardId);
    }
}
