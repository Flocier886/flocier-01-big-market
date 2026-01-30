package com.flocier.trigger.http;

import com.flocier.domain.activity.model.entity.UserRaffleOrderEntity;
import com.flocier.domain.activity.service.IRaffleActivityPartakeService;
import com.flocier.domain.activity.service.armory.IActivityArmory;
import com.flocier.domain.award.model.entity.UserAwardRecordEntity;
import com.flocier.domain.award.model.vo.AwardStateVO;
import com.flocier.domain.award.service.IAwardService;
import com.flocier.domain.strategy.model.entity.RaffleAwardEntity;
import com.flocier.domain.strategy.model.entity.RaffleFactorEntity;
import com.flocier.domain.strategy.service.IRaffleStrategy;
import com.flocier.domain.strategy.service.armory.IStrategyArmory;
import com.flocier.trigger.api.IRaffleActivityService;
import com.flocier.trigger.api.dto.ActivityDrawRequestDTO;
import com.flocier.trigger.api.dto.ActivityDrawResponseDTO;
import com.flocier.types.enums.ResponseCode;
import com.flocier.types.exception.AppException;
import com.flocier.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;

@RestController
@Slf4j
@RequestMapping("api/${app.config.api-version}/raffle/activity/")
@CrossOrigin("${app.config.cross-origin}")
public class RaffleActivityController implements IRaffleActivityService {
    @Resource
    private IActivityArmory activityArmory;
    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private IRaffleActivityPartakeService raffleActivityPartakeService;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Resource
    private IAwardService awardService;


    @Override
    @GetMapping("armory")
    public Response<Boolean> armory(@RequestParam Long activityId) {
        try {
            log.info("活动装配，数据预热，开始 activityId:{}", activityId);
            //活动sku库存预热
            activityArmory.assembleActivitySkuByActivityId(activityId);
            //活动策略预热
            strategyArmory.assembleLotteryStrategyByActivityId(activityId);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        }catch (Exception e){
            log.error("活动装配，数据预热，失败 activityId:{}", activityId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @Override
    @PostMapping("draw")
    public Response<ActivityDrawResponseDTO> draw(@RequestBody ActivityDrawRequestDTO request) {
        try {
            log.info("活动抽奖 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
            //参数校验
            String userId= request.getUserId();
            Long activityId=request.getActivityId();
            if(StringUtils.isBlank(userId) || activityId==null){
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            //参与活动
            UserRaffleOrderEntity orderEntity= raffleActivityPartakeService.createOrder(userId,activityId);
            log.info("活动抽奖，创建订单 userId:{} activityId:{} orderId:{}", request.getUserId(), request.getActivityId(), orderEntity.getOrderId());
            //抽奖策略
            RaffleAwardEntity awardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                    .strategyId(orderEntity.getStrategyId())
                    .userId(userId)
                    .endDateTime(orderEntity.getEndDateTime())
                    .build());
            //存放结果(奖品发放由MQ实现)
            UserAwardRecordEntity userAwardRecordEntity = UserAwardRecordEntity.builder()
                    .activityId(activityId)
                    .strategyId(orderEntity.getStrategyId())
                    .userId(userId)
                    .awardId(awardEntity.getAwardId())
                    .orderId(orderEntity.getOrderId())
                    .awardTitle(awardEntity.getAwardTitle())
                    .awardState(AwardStateVO.create)
                    .awardTime(new Date())
                    .build();
            awardService.saveUserAwardRecord(userAwardRecordEntity);
            //返回结果
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(ActivityDrawResponseDTO.builder()
                            .awardId(awardEntity.getAwardId())
                            .awardTitle(awardEntity.getAwardTitle())
                            .awardIndex(awardEntity.getSort())
                            .build())
                    .build();

        }catch (AppException e) {
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e){
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
