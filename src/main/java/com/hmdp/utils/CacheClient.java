package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * 改类是缓存工具类，用于和缓存相关的操作
 */
@Component
@Slf4j
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 可以将任意java对象存储进String类型的value中，并可以设置有效时间
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time,timeUnit);
    }

    /**
     * 可以将任意java对象存储进String类型的value中，并可以设置逻辑过期时间，用于处理缓存击穿问题
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void setWithLogic(String key,Object value,Long time,TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据key查询数据并反序列化为指定类型，当无该数据时将空值存储进redis中用于解决缓存穿透问题
     * @param prefix 前缀和id组合在一起构成key
     * @param id
     * @param dbFeedBack 由调用者指定查询数据库的逻辑，是查询商户还是其他逻辑
     * @param time
     * @param timeUnit
     * @param type 返回值类型
     * @return
     * @param <R> 由调用者指定id类型
     * @param <ID> 由调用者指定返回值类型
     */
    public <R,ID>R queryWithPassThrough(String prefix, ID id, Class<R>type,Function<ID,R> dbFeedBack,Long time,TimeUnit timeUnit){
        String key = prefix + id;

        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }

        if(json != null){
            return null;
        }

        R ret = dbFeedBack.apply(id);
        if(ret == null){
            set(key,"",time,timeUnit);
        }else{
            set(key,ret,time,timeUnit);
        }

        return ret;
    }

    /**
     * 根据key查询数据并反序列化为指定类型，并使用逻辑过期解决缓存击穿问题
     * @param prefix 前缀和id构成key
     * @param id
     * @param type
     * @param dbFeedBack 由调用者指定查询数据库的逻辑，是查询商户还是其他逻辑
     * @param time
     * @param timeUnit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R,ID>R queryWithLogicalExpire(String prefix,ID id,Class<R>type,Function<ID,R>dbFeedBack,Long time,TimeUnit timeUnit){
        String key = prefix + id;
        //查看Redis中是否有该商户
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            //Redis中有该商户，直接返回
            R cacheData = JSONUtil.toBean(json,type);
            return cacheData;
        }
        if(json != null){
            //shopJson是空字符串，说明数据库中也没有该店铺
            return null;
        }
        //开始缓存重建
        R ret = null;
        String lockKey = LOCK_SHOP_KEY + id;
        try {
            boolean lock = getLock(lockKey);
            if(!lock){
                //有其他线程在重建，进入等待
                Thread.sleep(50);
                return queryWithLogicalExpire(prefix,id,type,dbFeedBack,time,timeUnit);
            }
            ret = dbFeedBack.apply(id);
            if (ret == null) {
                //数据库中也没有该数据，查询错误,向Redis中插入空数据防止缓存穿透,并设置2min有效期
                set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            }else{
                setWithLogic(key,ret,CACHE_SHOP_TTL,TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            delLock(lockKey);
        }
        return ret;
    }

    //设置互斥锁防止缓存击穿,设置锁的有效期为10s，防止发生故障锁未释放
    public boolean getLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "0", 10L, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    //释放锁
    public void delLock(String key){
        stringRedisTemplate.delete(key);
    }
}
