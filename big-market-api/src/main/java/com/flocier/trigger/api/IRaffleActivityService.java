package com.flocier.trigger.api;

import com.flocier.trigger.api.dto.ActivityDrawRequestDTO;
import com.flocier.trigger.api.dto.ActivityDrawResponseDTO;
import com.flocier.types.model.Response;

public interface IRaffleActivityService {
    /**
     * 活动预热
     **/
    Response<Boolean> armory(Long activityId);

    /**
     *  活动抽奖总流程
     * */
    Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO activityDrawRequestDTO);
    /**
     * 日历签到返利接口
     *
     * @param userId 用户ID
     * @return 签到结果
     */
    Response<Boolean> calendarSignRebate(String userId);

}
