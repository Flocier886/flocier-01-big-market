package com.flocier.domain.activity.service.product;

import com.flocier.domain.activity.model.entity.SkuProductEntity;
import com.flocier.domain.activity.repository.IActivityRepository;
import com.flocier.domain.activity.service.IRaffleActivitySkuProductService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RaffleActivitySkuProductService implements IRaffleActivitySkuProductService {
    @Resource
    private IActivityRepository repository;


    @Override
    public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
        return repository.querySkuProductEntityListByActivityId(activityId);
    }
}
