package com.hmdp.service.impl;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
    static{
        redisScript = new DefaultRedisScript();
        redisScript.setLocation(new ClassPathResource("/RedisWork.lua"));
        redisScript.setResultType(Long.class);
    }
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private BlockingQueue<VoucherOrder> blockingQueue = new ArrayBlockingQueue<>(1024 * 1024);
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
            while(true){
                try {
                    VoucherOrder take = blockingQueue.take();
                    //创建订单
                    handleVoucherOrder(take);
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                }
            }
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
        //调用lua脚本判断库存是否充足以及该用户是否已经购买过该秒杀券
        Long userid = UserHolder.getUser().getId();
        Long flag = (Long)stringRedisTemplate.execute(redisScript, keys, userid + "");

        if(flag == 1){
            return Result.fail("库存不足");
        }
        if(flag == 2){
            return Result.fail("不能重复购买");
        }
        //异步处理订单
        long seckillVoucherOrderId = uniqueIdGenerator.generatorId(RedisConstants.SECKILL_VOUCHER_ID_PREFIX);
        //2.生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //2.1 填入订单id，使用全局唯一id生成器生成
        voucherOrder.setId(seckillVoucherOrderId);
        //2.2 填入用户id
        voucherOrder.setUserId(userid);
        //2.3 填入优惠券id
        voucherOrder.setVoucherId(id);
        //将订单放入阻塞队列,等待处理
        blockingQueue.add(voucherOrder);
        currentProxy = (ISeckillVoucherService) AopContext.currentProxy();

        return Result.ok(voucherOrder);
    }

    /**
     * 处理订单
     * @param voucherOrder
     */
    public void handleVoucherOrder(VoucherOrder voucherOrder){
        currentProxy.createVoucherOrder(voucherOrder);
    }

    /**
     * 更新数据库
     * @param voucherOrder
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder){
        //5.更新库存并使用乐观锁防止超卖问题
        boolean success = update().setSql("stock = stock - 1").eq("voucher_id", voucherOrder.getVoucherId()).gt("stock",0).update();
        voucherOrderMapper.insert(voucherOrder);
    }
}
