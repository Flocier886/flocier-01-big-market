package com.flocier.test.infrastructure;

import com.alibaba.fastjson.JSON;
import com.flocier.infrastructure.persistent.dao.IAwardDao;
import com.flocier.infrastructure.persistent.po.Award;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
@Slf4j
@RunWith(SpringRunner.class)
public class AwardDaoTest {
    @Resource
    private IAwardDao awardDao;

    @Test
    public void test_queryAwardList(){
        List<Award> awards=awardDao.queryAwardList();
        log.info("测试结果: {}", JSON.toJSONString(awards));
    }
}
