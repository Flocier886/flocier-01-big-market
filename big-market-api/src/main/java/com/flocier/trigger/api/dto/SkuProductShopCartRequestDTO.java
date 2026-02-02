package com.flocier.trigger.api.dto;

import lombok.Data;

@Data
public class SkuProductShopCartRequestDTO {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * sku 商品
     */
    private Long sku;

}
