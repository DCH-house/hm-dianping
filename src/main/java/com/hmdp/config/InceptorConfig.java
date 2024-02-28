package com.hmdp.config;

import com.hmdp.utils.UserLoginInteceptorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InceptorConfig implements WebMvcConfigurer {
    /**
     * 添加拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
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
                        );
    }
}
