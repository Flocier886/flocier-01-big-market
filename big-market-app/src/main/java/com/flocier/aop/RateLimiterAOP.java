package com.flocier.aop;

import com.flocier.types.annotations.NacosValue;
import com.flocier.types.annotations.RateLimiterAccessInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimiterAOP {

    @NacosValue("rateLimiterSwitch:open")
    private String rateLimiterSwitch;
    @Resource
    private RedissonClient redissonClient;

    @Pointcut("@annotation(com.flocier.types.annotations.RateLimiterAccessInterceptor)")
    public void aopPoint(){}

    @Around("aopPoint() && @annotation(rateLimiterAccessInterceptor)")
    public Object doRouter(ProceedingJoinPoint  jp, RateLimiterAccessInterceptor rateLimiterAccessInterceptor)throws Throwable{
        //验证限流开关是否打开
        if(StringUtils.isBlank(rateLimiterSwitch) || "close".equals(rateLimiterSwitch)){
            return jp.proceed();
        }

        String key=rateLimiterAccessInterceptor.key();
        if(StringUtils.isBlank(key)){
            throw new RuntimeException("annotation RateLimiter uId is null！");
        }
        //获取keyAttr字段
        String keyAttr=getAttrValue(key,jp.getArgs());
        log.info("aop attr {}", keyAttr);
        //黑名单拦截
        if(!"all".equals(keyAttr) && isInBlacklist(keyAttr,rateLimiterAccessInterceptor.blacklistCount())){
            log.info("限流-黑名单拦截(24h)：{}", keyAttr);
            return fallbackMethodResult(jp,rateLimiterAccessInterceptor.fallbackMethod());
        }
        //限流拦截
        if(!tryAcquireFromRedisson(keyAttr,rateLimiterAccessInterceptor.permitsPerSecond())){
            //黑名单计数累加
            if(rateLimiterAccessInterceptor.blacklistCount()!=0){
                addBlacklistCount(keyAttr);
            }
            log.info("限流-超频次拦截：{}", keyAttr);
            return fallbackMethodResult(jp,rateLimiterAccessInterceptor.fallbackMethod());
        }
        return jp.proceed();
    }


    private boolean tryAcquireFromRedisson(String keyAttr, long permitsPerSecond) {
        String limitKey="big_market_rate_limit_"+keyAttr;
        RRateLimiter rateLimiter=redissonClient.getRateLimiter(keyAttr);
        //尝试初始化限流配置，已经初始化便不会再初始化
        rateLimiter.trySetRate(RateType.OVERALL,permitsPerSecond,1, RateIntervalUnit.SECONDS);

        return rateLimiter.tryAcquire();
    }

    private boolean isInBlacklist(String keyAttr, long blacklistLimit) {
        if (blacklistLimit==0)return false;
        String redisKey="big_market_rate_blacklist_"+keyAttr;
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long count=counter.get();
        return count > blacklistLimit;
    }
    private void addBlacklistCount(String keyAttr) {
        String redisKey="big_market_rate_blacklist_"+keyAttr;
        RAtomicLong counter=redissonClient.getAtomicLong(redisKey);
        long count=counter.incrementAndGet();
        if (count==1){
            counter.expire(24, TimeUnit.HOURS);
        }
    }

    private Object fallbackMethodResult(JoinPoint jp, String fallbackMethod) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Signature signature=jp.getSignature();
        MethodSignature methodSignature=(MethodSignature) signature;
        Method method=jp.getTarget().getClass().getMethod(fallbackMethod,methodSignature.getParameterTypes());
        return method.invoke(jp.getThis(),jp.getArgs());
    }

    private String getAttrValue(String attr, Object[] args) {
        if(args[0] instanceof String){
            return args[0].toString();
        }
        String fieldValue=null;
        for (Object arg:args){
            try {
                if(StringUtils.isNotBlank(fieldValue)){
                    break;
                }
                fieldValue=String.valueOf(this.getValueByName(arg,attr));
            }catch (Exception e){
                log.error("获取路由属性值失败 attr：{}", attr, e);
            }
        }
        return fieldValue;
    }

    private Object getValueByName(Object arg, String attr) {
        try {
            Field field=getFieldByName(arg,attr);
            if(field==null){
                return null;
            }
            field.setAccessible(true);
            Object o=field.get(arg);
            field.setAccessible(false);
            return o;
        } catch (IllegalAccessException e){
            return null;
        }
    }

    private Field getFieldByName(Object arg, String attr) {
        try {
            Field field;
            try {
                field=arg.getClass().getDeclaredField(attr);
            }catch (NoSuchFieldException e){
                field=arg.getClass().getSuperclass().getDeclaredField(attr);
            }
            return field;
        }catch (NoSuchFieldException e){
            return null;
        }
    }
}
