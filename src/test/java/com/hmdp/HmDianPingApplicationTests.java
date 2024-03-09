package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class HmDianPingApplicationTests {
    @Test
    public void testCase(){
        long epochSecond = LocalDateTime.of(2024, 01, 01, 0, 0, 0).toEpochSecond(ZoneOffset.UTC);
        System.out.println(epochSecond);
    }
}

