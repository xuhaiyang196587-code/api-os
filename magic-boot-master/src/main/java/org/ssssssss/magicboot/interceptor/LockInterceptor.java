package org.ssssssss.magicboot.interceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.config.MagicFunction;
import org.ssssssss.magicapi.core.context.RequestContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.script.annotation.Function;

import jakarta.annotation.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Configuration
@Slf4j
@Order(999) //低优先级
public class LockInterceptor implements RequestInterceptor {

    @Resource
    private MagicResourceService magicResourceService;

    private Map<String,ReentrantLock> lockMap;

    private ThreadLocal<ReentrantLock> threadLocal= new ThreadLocal<>();

    @Override
    public Object preHandle(RequestEntity requestEntity) throws Exception {
        //获取lock参数
        String enableLock = requestEntity.getApiInfo().getOptionValue("lock");
        if(enableLock == null){//若未设置lock参数，则退出
            return null;
        }
        enableLock = enableLock.trim();
        if(enableLock.matches("^[-\\+]?[\\d]+$")){
            long ms = Long.parseLong(enableLock);//获取同步锁超时时间（毫秒）
            ReentrantLock lock = this.getLock(requestEntity);
            if (ms<0){//无超时则一直循环等待
                while(!lock.tryLock(100,TimeUnit.MILLISECONDS));
            }else{
                if(!lock.tryLock(ms,TimeUnit.MILLISECONDS)){//超时退出
                    return new JsonBean<>(401, "已超时");
                }
            }
            threadLocal.set(lock);//成功获取到锁资源，将锁加入ThreadLock
        }
        return null;
    }

    @Override
    public void afterCompletion(RequestEntity requestEntity, Object returnValue, Throwable throwable) {
        //从ThreadLock获取锁，并释放
        ReentrantLock lock = threadLocal.get();
        if (lock != null){
            lock.unlock();
            log.info("{}已释放同步锁",Thread.currentThread().getId());
        }
    }

    /**
     * 获取接口对应锁
     * @param requestEntity
     * @return
     */
    private ReentrantLock getLock(RequestEntity requestEntity){
        ApiInfo entity = requestEntity.getApiInfo();
        String path = "/"+magicResourceService.getGroupPath(entity.getGroupId())+"/"+entity.getPath();
        if(lockMap==null){
            lockMap= new HashMap<>();
        }
        ReentrantLock lock = lockMap.get(path);
        if(lock==null){
            lock=new ReentrantLock();
            lockMap.put(path,lock);
        }
        return lock;
    }

    @Bean("lockMagicFunction")
    public MagicFunction getMagicFunction(){
        return new MagicFunction() {
            @Function
            public void lock() throws InterruptedException {
                ReentrantLock lock = threadLocal.get();
                if (lock != null){
                    return;
                }
                RequestEntity entity = RequestContext.getRequestEntity();
                lock = getLock(entity);
                while(!lock.tryLock(100,TimeUnit.MILLISECONDS));
                threadLocal.set(lock);
                log.info("{}已加锁",Thread.currentThread().getId());
            }
            @Function
            public void unlock() throws InterruptedException {
                RequestEntity entity = RequestContext.getRequestEntity();
                ReentrantLock lock = getLock(entity);
                lock.unlock();
                threadLocal.remove();
                log.info("{}已释放",Thread.currentThread().getId());
            }
        };
    }
}
