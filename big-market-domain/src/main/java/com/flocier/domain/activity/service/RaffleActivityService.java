package com.flocier.domain.activity.service;

import com.flocier.domain.activity.repository.IActivityRepository;
import org.springframework.stereotype.Service;


@Service
public class RaffleActivityService extends AbstractRaffleActivity{
    public RaffleActivityService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

}
