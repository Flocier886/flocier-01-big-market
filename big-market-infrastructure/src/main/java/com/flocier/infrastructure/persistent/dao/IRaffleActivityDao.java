package com.flocier.infrastructure.persistent.dao;

import com.flocier.infrastructure.persistent.po.RaffleActivity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IRaffleActivityDao {
    RaffleActivity queryRaffleActivityByActivityId(Long activityId);
}
