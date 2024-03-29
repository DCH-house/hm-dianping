package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.VoucherOrder;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务类
 * </p>
 *
 * @author dch
 * @since 2024-03-08
 */
public interface ISeckillVoucherService extends IService<SeckillVoucher> {
    Result seckillVoucher(Long id);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
