package com.hmdp.config;

import com.hmdp.utils.RefreshInterceptor;
import com.hmdp.utils.UserLoginInteceptorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class InceptorConfig implements WebMvcConfigurer {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    /**
     * 添加拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(new UserLoginInteceptorHandler())
                .excludePathPatterns(
                        //放行请求，这些请求不做拦截
                        "/user/login",
                        "/user/code",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/voucher/**",
                        "/voucher-order/**"
                        ).order(1);
        //token刷新拦截器
        registry.addInterceptor(new RefreshInterceptor(stringRedisTemplate))
                .addPathPatterns("/**").order(0);
    }
}
