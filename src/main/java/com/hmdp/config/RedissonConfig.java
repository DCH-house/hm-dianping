//package com.hmdp.config;
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 配置Redisson
// */
//@Configuration
//public class RedissonConfig {
//    @Bean
//    public RedissonClient getRedisson(){
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis:192.168.21.128:6379").setPassword("dch");
//
//        return Redisson.create(config);
//    }
//}
