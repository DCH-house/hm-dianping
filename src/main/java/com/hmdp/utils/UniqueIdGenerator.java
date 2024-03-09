package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * 本类用于生成全局唯一ID
 */
@Component
public class UniqueIdGenerator {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 2024.01.01 00:00:00作为基准时间
     */
    private final long TIME_BEGIN = 1704067200L;

    /**
     * 根据不同的业务生成不同的id
     * @param keyPrefix 业务
     * @return
     */
    public long generatorId(String keyPrefix){
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long interval = now - TIME_BEGIN;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd");
        String timeStamp = simpleDateFormat.format(new Date());
        //为了便于管理和查询，将当日时间融入key中
        String key = keyPrefix + timeStamp;
        Long count = stringRedisTemplate.opsForValue().increment(key);

        Long id = interval << 32 | count;

        return id;
    }
}
