package com.flocier.infrastructure.persistent.dao;

import cn.bugstack.middleware.db.router.annotation.DBRouter;
import com.flocier.infrastructure.persistent.po.RaffleActivityAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IRaffleActivityAccountDao {
    int updateAccountQuota(RaffleActivityAccount raffleActivityAccount);

    void insert(RaffleActivityAccount raffleActivityAccount);

    @DBRouter
    RaffleActivityAccount queryActivityAccountByUserId(RaffleActivityAccount raffleActivityAccountReq);

    void updateActivityAccountDaySurplusImageQuota(RaffleActivityAccount build);

    void updateActivityAccountMonthSurplusImageQuota(RaffleActivityAccount build);

    int updateActivityAccountSubtractionQuota(RaffleActivityAccount build);

    int updateActivityAccountMonthSubtractionQuota(RaffleActivityAccount raffleActivityAccount);

    int updateActivityAccountDaySubtractionQuota(RaffleActivityAccount raffleActivityAccount);

    RaffleActivityAccount queryAccountByUserId(RaffleActivityAccount raffleActivityAccount);
}
