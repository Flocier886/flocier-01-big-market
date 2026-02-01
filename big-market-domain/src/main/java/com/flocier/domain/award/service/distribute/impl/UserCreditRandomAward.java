package com.flocier.domain.award.service.distribute.impl;

import com.flocier.domain.award.model.aggregate.GiveOutPrizesAggregate;
import com.flocier.domain.award.model.entity.DistributeAwardEntity;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;
import com.flocier.domain.award.model.entity.UserCreditAwardEntity;
import com.flocier.domain.award.model.vo.AwardStateVO;
import com.flocier.domain.award.repository.IAwardRepository;
import com.flocier.domain.award.service.distribute.IDistributeAward;
import com.flocier.types.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.locks.Condition;

@Component("user_credit_random")
public class UserCreditRandomAward implements IDistributeAward {
    @Resource
    private IAwardRepository repository;

    @Override
    public void giveOutPrizes(DistributeAwardEntity distributeAwardEntity) {
        Integer awardId= distributeAwardEntity.getAwardId();
        String awardConfig= distributeAwardEntity.getAwardConfig();
        //如果配置为空就再次查询
        if(StringUtils.isBlank(awardConfig)){
            awardConfig=repository.queryAwardConfig(awardId);
        }
        String[] creditRange=awardConfig.split(Constants.SPLIT);
        if(creditRange.length!=2){
            throw new RuntimeException("award_config 「" + awardConfig + "」配置不是一个范围值，如 1,100");
        }
        //生成随机积分值
        BigDecimal creditAmount=generateRandom(new BigDecimal(creditRange[0]),new BigDecimal(creditRange[1]));
        //构建订单+账户聚合对象
        UserAwardRecordEntity userAwardRecordEntity = GiveOutPrizesAggregate.buildDistributeUserAwardRecordEntity(
                distributeAwardEntity.getUserId(),
                distributeAwardEntity.getOrderId(),
                distributeAwardEntity.getAwardId(),
                AwardStateVO.complete
        );
        UserCreditAwardEntity userCreditAwardEntity = GiveOutPrizesAggregate.buildUserCreditAwardEntity(distributeAwardEntity.getUserId(), creditAmount);
        GiveOutPrizesAggregate giveOutPrizesAggregate = new GiveOutPrizesAggregate();
        giveOutPrizesAggregate.setUserId(distributeAwardEntity.getUserId());
        giveOutPrizesAggregate.setUserAwardRecordEntity(userAwardRecordEntity);
        giveOutPrizesAggregate.setUserCreditAwardEntity(userCreditAwardEntity);
        // 存储发奖对象
        repository.saveGiveOutPrizesAggregate(giveOutPrizesAggregate);

    }

    private BigDecimal generateRandom(BigDecimal min, BigDecimal max) {
        if(min.equals(max))return min;
        BigDecimal randomBigDecimal=min.add(BigDecimal.valueOf(Math.random()).multiply(max.subtract(min)));
        return randomBigDecimal.round(new MathContext(3));
    }
}
