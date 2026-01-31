package com.flocier.trigger.api.dto;

import lombok.Data;

@Data
public class UserActivityAccountRequestDTO {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}
