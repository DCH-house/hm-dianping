package com.hmdp.service.impl;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author dch
 * @since 2024-2-27
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    public Result sendCode(String phone, HttpSession session){
        //校验手机号是否合法
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //如果不合法
        if(phoneInvalid){
            return Result.fail("手机号不合法,请重新输入");
        }
        //如果合法，则生成验证码(一个六位数随机数),并保存在session中
        String code = RandomUtil.randomNumbers(6);
        session.setAttribute("code",code);
        log.debug("成功发送验证码：" + code);
        return Result.ok();
    }
}
