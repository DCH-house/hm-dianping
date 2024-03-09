package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 该类专门用于Redis设置逻辑过期
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
