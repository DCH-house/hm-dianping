package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dch
 * @since 2024-02-29
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public Result queryTypeList(){
        String shopTypeListKey = RedisConstants.SHOP_TYPE_LIST;
        //查看Redis中有无数据
        Long size = stringRedisTemplate.opsForList().size(shopTypeListKey);
        if(size != 0){
            List<String> cacheShopTypeList = stringRedisTemplate.opsForList().range(shopTypeListKey, 0, size);
            List<ShopType>list = new ArrayList<>();
            for(String shopType : cacheShopTypeList){
                ShopType type = JSONUtil.toBean(shopType, ShopType.class);
                list.add(type);
            }
            return Result.ok(list);
        }
        //Redis中没有数据
        List<ShopType> databaseShopTypeList = query().orderByAsc("sort").list();
        if(databaseShopTypeList == null){
            return Result.fail("错误,无法获取商户类型列表");
        }
        for(int i = 0; i < databaseShopTypeList.size(); i ++){
            String shopType = JSONUtil.toJsonStr(databaseShopTypeList.get(i));
            stringRedisTemplate.opsForList().leftPush(shopTypeListKey,shopType);
        }

        return Result.ok(databaseShopTypeList);
    }
}
