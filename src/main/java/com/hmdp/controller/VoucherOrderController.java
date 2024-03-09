package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.impl.SeckillVoucherServiceImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author dch
 * @since 2024-03-08
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        return iSeckillVoucherService.seckillVoucher(voucherId);
    }
}
