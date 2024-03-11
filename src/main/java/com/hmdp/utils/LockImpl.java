package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class LockImpl implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String LOCK_PREFIX = UUID.randomUUID().toString(true) + "-";
    private String lockId = LOCK_PREFIX + "-" + Thread.currentThread().getId();
    private static DefaultRedisScript redisScript;
    static{
        redisScript = new DefaultRedisScript();
        redisScript.setLocation(new ClassPathResource("unlock.lua"));
        redisScript.setResultType(Long.class);
    }
    public LockImpl(String name,StringRedisTemplate stringRedisTemplate){
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 能否获取锁
     * @param timeout 超时时间
     * @return
     */
    public boolean tryLock(Long timeout){
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, lockId, timeout, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    /**
     * 利用lua脚本释放锁
     */
    public void unLock(){
        stringRedisTemplate.execute(redisScript, Collections.singletonList(KEY_PREFIX + name), lockId);
    }
}
