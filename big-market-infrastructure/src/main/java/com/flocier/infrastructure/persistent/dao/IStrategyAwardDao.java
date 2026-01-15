package com.flocier.infrastructure.persistent.dao;

import com.flocier.infrastructure.persistent.po.StrategyAward;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IStrategyAwardDao {
    List<StrategyAward> queryStrategyAwardListByStrategyId(Long strategyId);
}
