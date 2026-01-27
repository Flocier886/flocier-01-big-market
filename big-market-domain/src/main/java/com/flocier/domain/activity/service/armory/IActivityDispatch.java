package com.flocier.domain.activity.service.armory;

import java.util.Date;

public interface IActivityDispatch {
    /**
     * 扣减库存操作
     * */
    boolean subtractionActivitySkuStock(Long sku, Date endDateTime);
}
