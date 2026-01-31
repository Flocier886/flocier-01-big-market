package com.flocier.trigger.api;

import com.flocier.trigger.api.dto.ActivityDrawRequestDTO;
import com.flocier.trigger.api.dto.ActivityDrawResponseDTO;
import com.flocier.trigger.api.dto.UserActivityAccountRequestDTO;
import com.flocier.trigger.api.dto.UserActivityAccountResponseDTO;
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

    /**
     * 判断是否完成日历签到返利接口
     *
     * @param userId 用户ID
     * @return 签到结果 true 已签到，false 未签到
     */
    Response<Boolean>isCalendarSignRebate(String userId);

    /**
     * 查询用户活动账户
     *
     * @param request 请求对象「活动ID、用户ID」
     * @return 返回结果「总额度、月额度、日额度」
     */
    Response<UserActivityAccountResponseDTO> queryUserActivityAccount(UserActivityAccountRequestDTO request);

}
