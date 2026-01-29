package com.flocier.trigger.api.dto;

import lombok.Data;

import javax.annotation.security.DenyAll;

@Data
public class ActivityDrawRequestDTO {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;
}
