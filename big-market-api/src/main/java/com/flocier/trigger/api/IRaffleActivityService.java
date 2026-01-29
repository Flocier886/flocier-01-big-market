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
}
