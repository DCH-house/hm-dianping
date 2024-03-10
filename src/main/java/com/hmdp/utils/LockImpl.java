package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class LockImpl implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    public LockImpl(String name,StringRedisTemplate stringRedisTemplate){
        this.name = KEY_PREFIX + name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 能否获取锁
     * @param timeout 超时时间
     * @return
     */
    public boolean tryLock(Long timeout){
        String thread = Thread.currentThread().getName();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(name, thread, timeout, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     */
    public void unLock(){
        stringRedisTemplate.delete(name);
    }
}
