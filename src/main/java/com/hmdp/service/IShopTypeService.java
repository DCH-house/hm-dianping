package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author dch
 * @since 2024-02-29
 */
public interface IShopTypeService extends IService<ShopType> {
    Result queryTypeList();
}
