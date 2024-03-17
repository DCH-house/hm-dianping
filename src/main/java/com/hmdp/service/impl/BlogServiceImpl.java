package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author dch
 * @since 2024-03-17
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    /**
     * 根据博文id获取博文
     */
    public Result getBlogById(String id){
        Blog blog = baseMapper.selectById(id);
        if(blog == null){
            return Result.fail("该博文不存在");
        }
        return Result.ok(blog);
    }
}
