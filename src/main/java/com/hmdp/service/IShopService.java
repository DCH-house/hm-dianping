package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dch
 * @since 2024-02-29
 */
public interface IShopService extends IService<Shop> {
    Result queryShopById(Long id);

    Result updateShop(Shop shop);
}
