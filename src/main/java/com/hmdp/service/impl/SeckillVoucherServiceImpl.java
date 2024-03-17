package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.LockImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UniqueIdGenerator;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author dch
 * @since 2024-03-08
 */
@Service
@Slf4j
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {
    @Resource
    private UniqueIdGenerator uniqueIdGenerator;
    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    private static DefaultRedisScript redisScript;
    private String messageStream = "stream.orders";

    static{
        redisScript = new DefaultRedisScript();
        redisScript.setLocation(new ClassPathResource("/RedisWork.lua"));
        redisScript.setResultType(Long.class);
    }
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 创建一个线程
     */
    private static ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    private ISeckillVoucherService currentProxy;

    @PostConstruct
    public void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        //项目一启动就检查阻塞队列中是否有未处理的订单，有就进行处理
        public void run(){
            handleVoucherOrder();
        }
    }
    public Result seckillVoucher(Long id){
        //1.根据id从数据库中查询对应的优惠券
        SeckillVoucher seckillVoucher = getById(id);
        //2.判断该优惠券是否开始以及结束
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("活动未开始");
        }
        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("活动已结束");
        }
        List<String> keys = new ArrayList<>();
        keys.add(RedisConstants.SECKILL_STOCK_KEY + id);
        keys.add(RedisConstants.SECKILL_VOUCHER_KEY + id);
        Long userid = UserHolder.getUser().getId();
        long seckillVoucherOrderId = uniqueIdGenerator.generatorId(RedisConstants.SECKILL_VOUCHER_ID_PREFIX);
        //调用lua脚本判断库存是否充足以及该用户是否已经购买过该秒杀券,并将满足条件的订单加入消息队列
        Long flag = (Long)stringRedisTemplate.execute(redisScript, keys, userid + "",id + "",seckillVoucherOrderId + "");

        if(flag == 1){
            return Result.fail("库存不足");
        }
        if(flag == 2){
            return Result.fail("不能重复购买");
        }
        //2.生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.1 填入订单id，使用全局唯一id生成器生成
        voucherOrder.setId(seckillVoucherOrderId);
        //2.2 填入用户id
        voucherOrder.setUserId(userid);
        //2.3 填入优惠券id
        voucherOrder.setVoucherId(id);
        currentProxy = (ISeckillVoucherService) AopContext.currentProxy();

        return Result.ok(voucherOrder);
    }

    /**
     * 不断从消息队列中读取消息
     */
    public void handleVoucherOrder(){
        while(true){
            try {
                //读取消息
                List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(1),
                        StreamOffset.create(messageStream, ReadOffset.lastConsumed()));
                if(read.isEmpty() || read == null){
                    //未读到消息
                    continue;
                }

                MapRecord<String, Object, Object> message = read.get(0);
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(message.getValue(), new VoucherOrder(),true);
                //ACK确认，注意：第137行和138行不可交换顺序，否则当更新数据库操作失败时，该订单会不翼而飞
                currentProxy.createVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForStream().acknowledge(messageStream,"g1",message.getId());
            } catch (Exception e) {
                handlePaddinList();
                log.error("处理订单异常");
            }
        }
    }

    /**
     * 处理订单过程发送异常，恢复时需要处理paddin-list中的消息
     */
    public void handlePaddinList(){
        while(true){
            try {
                //读取消息
                List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(messageStream, ReadOffset.from("0")));
                if(read.isEmpty() || read == null){
                    //未读到消息
                    break;
                }

                MapRecord<String, Object, Object> message = read.get(0);
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(message.getValue(), new VoucherOrder(),true);
                //ACK确认
                currentProxy.createVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForStream().acknowledge(messageStream,"g1",message.getId());
            } catch (Exception e) {
                log.error("处理padding-list异常");
            }
        }
    }
    /**
     * 更新数据库
     * @param voucherOrder
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder){
        //更新库存
        boolean success = update().setSql("stock = stock - 1").eq("voucher_id", voucherOrder.getId()).update();
        voucherOrderMapper.insert(voucherOrder);
    }
}
