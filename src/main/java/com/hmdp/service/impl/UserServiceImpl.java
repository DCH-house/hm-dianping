package com.hmdp.service.impl;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

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
    /**
     * 发送验证码
     * @param phone 手机号
     * @param session
     * @return 是否正常发送
     */
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

    /**
     * 用户登录功能
     * @param loginFormDTO
     * @param session
     * @return
     */
    public Result login(LoginFormDTO loginFormDTO,HttpSession session){
        //验证手机号是否正确
        String phone = loginFormDTO.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //验证码是否一致
        String code = loginFormDTO.getCode();
        String cachecode = (String)session.getAttribute("code");
        if(code == null || !cachecode.equals(code)){
            return Result.fail("验证码错误");
        }
        //数据库中是否有该用户
        User user = query().eq("phone", phone).one();
        if(user == null){
            user = createUserWithPhone(phone);
            //保存用户
            save(user);
        }
        //将用户保存到session中
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }

    /**
     * 创建一个用户
     * @param phone 手机号
     * @return 用户
     */
    public User createUserWithPhone(String phone){
        User user = new User();
        user.setPhone(phone);
        //随机生成一个昵称
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        return user;
    }
}
