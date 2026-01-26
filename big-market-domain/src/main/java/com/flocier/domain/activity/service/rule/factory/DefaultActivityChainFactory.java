package com.flocier.domain.activity.service.rule.factory;

import com.flocier.domain.activity.service.rule.IActionChain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class DefaultActivityChainFactory {
    private final IActionChain actionChain;

    public DefaultActivityChainFactory(Map<String, IActionChain>actionChainMap){
        actionChain=actionChainMap.get(ActionModel.activity_base_action.code);
        actionChain.appendNext(actionChainMap.get(ActionModel.activity_sku_stock_action.code));
    }
    public IActionChain openActionChain(){
        return this.actionChain;
    }

    @Getter
    @AllArgsConstructor
    public enum ActionModel{
        activity_base_action("activity_base_action", "活动的库存、时间校验"),
        activity_sku_stock_action("activity_sku_stock_action", "活动sku库存"),
        ;
        private final String code;
        private final String info;
    }
}
