package com.flocier.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.flocier.types.annotations.NacosValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
public class NacosValueBeanFactory implements BeanPostProcessor {
    private static final String GROUP="BIG_MARKET_GROUP";
    private static final String PREFIX="big-market-dcc_";

    private ConfigService configService;
    //映射dataId->bean
    private final Map<String, Object>dccObjGroup=new HashMap<>();

    @PostConstruct
    public void init()throws Exception{
        Properties properties=new Properties();
        properties.put("serverAddr","192.168.100.128:8848");
        properties.put("namespace","93524184-0f4b-4f2d-b7cf-6831631f39ea");
        configService= NacosFactory.createConfigService(properties);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean,String beanName) throws BeansException {
        Class<?> clazz=bean.getClass();
        Object beanObject=bean;
        if(AopUtils.isAopProxy(bean)){
            clazz=AopUtils.getTargetClass(bean);
            beanObject= AopProxyUtils.getSingletonTarget(bean);
        }
        final Object targetBeanObject=beanObject;
        for (Field field:clazz.getDeclaredFields()){
            if(!field.isAnnotationPresent(NacosValue.class)){
                continue;
            }
            NacosValue nac=field.getAnnotation(NacosValue.class);
            String[] arr=nac.value().split(":");
            String key=arr[0];
            String defaultValue=arr.length == 2 ? arr[1] : null;
            String dataId=PREFIX+key;
            try {
                //先尝试从nacos中拉取值
                String configValue=configService.getConfig(dataId,GROUP,3000);
                //如果没有配置则创建配置
                if(StringUtils.isBlank(configValue)){
                    configService.publishConfig(dataId,GROUP,defaultValue);
                    if(StringUtils.isNotBlank(defaultValue)){
                        configValue=defaultValue;
                    }
                }
                field.setAccessible(true);
                field.set(targetBeanObject,configValue);
                field.setAccessible(false);
                log.info("Nacos 设置配置 field={} value={}", field.getName(), configValue);
                //添加监听器，动态更新
                configService.addListener(dataId, GROUP, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String newValue) {
                        try {
                            log.info("Nacos 动态更新 key={} newValue={}", key, newValue);
                            Field f=targetBeanObject.getClass().getDeclaredField(field.getName());
                            f.setAccessible(true);
                            f.set(targetBeanObject,newValue);
                            f.setAccessible(false);
                        }catch (Exception e){
                            log.error("NAC 动态刷新失败", e);
                        }
                    }
                });
                dccObjGroup.put(dataId,targetBeanObject);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
        return bean;
    }
}
