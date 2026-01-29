package com.flocier.domain.activity.service.partake;

import com.alibaba.fastjson.JSON;
import com.flocier.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.flocier.domain.activity.model.entity.ActivityEntity;
import com.flocier.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.flocier.domain.activity.model.entity.UserRaffleOrderEntity;
import com.flocier.domain.activity.model.vo.ActivityStateVO;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.activity.service.IRaffleActivityPartakeService;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public abstract class AbstractRaffleActivityPartake implements IRaffleActivityPartakeService {
    protected final IActivityRepository activityRepository;

    protected AbstractRaffleActivityPartake(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity){
        //基础信息
        String userId= partakeRaffleActivityEntity.getUserId();
        Long activityId= partakeRaffleActivityEntity.getActivityId();
        Date now =new Date();
        //查询活动
        ActivityEntity activityEntity=activityRepository.queryRaffleActivityByActivityId(activityId);
        //校验活动状态与活动时间
        if(!activityEntity.getState().equals(ActivityStateVO.open)){
            throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(), ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
        }
        if (activityEntity.getBeginDateTime().after(now) || activityEntity.getEndDateTime().before(now)){
            throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(), ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
        }
        //查询是否有未被使用的活动订单记录
        UserRaffleOrderEntity userRaffleOrderEntity=activityRepository.queryNoUsedRaffleOrder(partakeRaffleActivityEntity);
        if (userRaffleOrderEntity!=null){
            log.info("创建参与活动订单 userId:{} activityId:{} userRaffleOrderEntity:{}", userId, activityId, JSON.toJSONString(userRaffleOrderEntity));
            return userRaffleOrderEntity;
        }
        //额度账户过滤与构建订单实体对象
        CreatePartakeOrderAggregate createPartakeOrderAggregate=this.doFilterAccount(userId,activityId,now);
        //构建订单
        UserRaffleOrderEntity userRaffleOrder=this.buildUserRaffleOrder(userId, activityId, now);
        //填充实体对象
        createPartakeOrderAggregate.setUserRaffleOrderEntity(userRaffleOrder);
        //保存聚合对象，实现订单保存与次数更新
        activityRepository.saveCreatePartakeOrderAggregate(createPartakeOrderAggregate);
        //返回订单信息
        return userRaffleOrder;
    }

    @Override
    public UserRaffleOrderEntity createOrder(String userId, Long activityId) {
        PartakeRaffleActivityEntity partakeRaffleActivityEntity=new PartakeRaffleActivityEntity();
        partakeRaffleActivityEntity.setUserId(userId);
        partakeRaffleActivityEntity.setActivityId(activityId);
        return createOrder(partakeRaffleActivityEntity);
    }

    protected abstract UserRaffleOrderEntity buildUserRaffleOrder(String userId, Long activityId, Date now);

    protected abstract CreatePartakeOrderAggregate doFilterAccount(String userId, Long activityId, Date now);
}
