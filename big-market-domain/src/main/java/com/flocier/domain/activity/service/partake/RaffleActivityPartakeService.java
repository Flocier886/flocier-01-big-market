package com.flocier.domain.activity.service.partake;

import com.flocier.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.flocier.domain.activity.model.entity.*;
import com.flocier.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class RaffleActivityPartakeService extends AbstractRaffleActivityPartake{
    private final SimpleDateFormat dateFormatMonth = new SimpleDateFormat("yyyy-MM");
    private final SimpleDateFormat dateFormatDay = new SimpleDateFormat("yyyy-MM-dd");


    protected RaffleActivityPartakeService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

    @Override
    protected UserRaffleOrderEntity buildUserRaffleOrder(String userId, Long activityId, Date now) {
        ActivityEntity activityEntity=activityRepository.queryRaffleActivityByActivityId(activityId);
        //构建订单
        return UserRaffleOrderEntity.builder()
                .userId(userId)
                .activityId(activityId)
                .activityName(activityEntity.getActivityName())
                .strategyId(activityEntity.getStrategyId())
                .orderId(RandomStringUtils.randomNumeric(12))
                .orderState(UserRaffleOrderStateVO.create)
                .endDateTime(activityEntity.getEndDateTime())
                .orderTime(now)
                .build();
    }

    @Override
    protected CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, Date now) {
        //查询账户总额度
        ActivityAccountEntity activityAccountEntity=activityRepository.queryActivityAccountByUserId(userId, activityId);
        //判断剩余额度
        if (activityAccountEntity ==null || activityAccountEntity.getTotalCountSurplus()<=0){
            throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
        }
        //查询账户月额度
        String month = dateFormatMonth.format(now);
        ActivityAccountMonthEntity activityAccountMonthEntity=activityRepository.queryActivityAccountMonthByUserId(userId,activityId,month);
        if(activityAccountMonthEntity!=null && activityAccountMonthEntity.getMonthCountSurplus()<=0){
            throw new AppException(ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getInfo());
        }
        //判断是否存在并选择创建
        boolean isExistAccountMonth=null!=activityAccountMonthEntity;
        if(activityAccountMonthEntity==null){
            activityAccountMonthEntity = new ActivityAccountMonthEntity();
            activityAccountMonthEntity.setUserId(userId);
            activityAccountMonthEntity.setActivityId(activityId);
            activityAccountMonthEntity.setMonth(month);
            activityAccountMonthEntity.setMonthCount(activityAccountEntity.getMonthCount());
            activityAccountMonthEntity.setMonthCountSurplus(activityAccountEntity.getMonthCountSurplus());
        }
        //查询账户日额度
        String day=dateFormatDay.format(now);
        ActivityAccountDayEntity activityAccountDayEntity=activityRepository.queryActivityAccountDayByUserId(userId, activityId, day);
        if(activityAccountDayEntity!=null && activityAccountDayEntity.getDayCountSurplus()<=0){
            throw new AppException(ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getInfo());
        }
        //判断是否存在并选择创建
        boolean isExistAccountDay=null!=activityAccountDayEntity;
        if(activityAccountDayEntity==null){
            activityAccountDayEntity = new ActivityAccountDayEntity();
            activityAccountDayEntity.setUserId(userId);
            activityAccountDayEntity.setActivityId(activityId);
            activityAccountDayEntity.setDay(day);
            activityAccountDayEntity.setDayCount(activityAccountEntity.getDayCount());
            activityAccountDayEntity.setDayCountSurplus(activityAccountEntity.getDayCountSurplus());
        }
        //创建对象并返回
        return CreatePartakeOrderAggregate.builder()
                .userId(userId)
                .activityId(activityId)
                .activityAccountEntity(activityAccountEntity)
                .isExistAccountMonth(isExistAccountMonth)
                .activityAccountMonthEntity(activityAccountMonthEntity)
                .isExistAccountDay(isExistAccountDay)
                .activityAccountDayEntity(activityAccountDayEntity)
                .build();
    }
}
