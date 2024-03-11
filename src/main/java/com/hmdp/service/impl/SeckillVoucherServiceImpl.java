package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.LockImpl;
import com.hmdp.utils.UniqueIdGenerator;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author dch
 * @since 2024-03-08
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {
    @Resource
    private UniqueIdGenerator uniqueIdGenerator;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
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
        //3.查询库存是否充足
        Integer stock = seckillVoucher.getStock();
        if(stock < 1){
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //可以设置锁的重试时间(默认为-1表示不允许重试)、超时时间(默认30分钟)
        boolean isLock = lock.tryLock();
        if(!isLock){
            return Result.fail("每个用户只能购买一次");
        }
        try {
            //获取代理对象，避免自身调用导致事务失效
            ISeckillVoucherService currentProxy = (ISeckillVoucherService) AopContext.currentProxy();
            return currentProxy.createVoucherOrder(id);
        } finally {
            //释放锁
            lock.unlock();
        }
//        LockImpl lock = new LockImpl("order:" + userId, stringRedisTemplate);
//        if(!lock.tryLock(5L)){
//            return Result.fail("每个用户只能购买一次");
//        }
//        try {
//            //获取代理对象，避免自身调用导致事务失效
//            ISeckillVoucherService currentProxy = (ISeckillVoucherService) AopContext.currentProxy();
//            return currentProxy.createVoucherOrder(id);
//        } finally {
//            //释放锁
//            lock.unLock();
//        }
    }
    @Transactional
    public Result createVoucherOrder(Long id){
        //4.一人一单判断
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("voucher_id", id).count();
        if(count > 0){
            //该用户已经下过单
            return Result.fail("每个用户只能购买一次");
        }
        //5.更新库存并使用乐观锁防止超卖问题
        boolean success = update().setSql("stock = stock - 1").eq("voucher_id", id).gt("stock",0).update();
        if(!success){
            return Result.fail("库存不足");
        }
        //6.生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1 填入订单id，使用全局唯一id生成器生成
        long voucherOrderId = uniqueIdGenerator.generatorId("voucherOrder");
        voucherOrder.setId(voucherOrderId);
        //6.2 填入用户id
        voucherOrder.setUserId(userId);
        //6.3 填入优惠券id
        voucherOrder.setVoucherId(id);

        return Result.ok(voucherOrder);
    }
}
