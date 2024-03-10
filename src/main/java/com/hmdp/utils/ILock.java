package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeout 超时时间
     * @return 返回获取锁是否成功 成功True 失败False
     */
    boolean tryLock(Long timeout);

    /**
     * 释放锁
     */
    void unLock();
}
