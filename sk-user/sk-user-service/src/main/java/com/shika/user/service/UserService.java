package com.shika.user.service;

import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.utils.NumberUtils;
import com.shika.user.mapper.UserMapper;
import com.shika.user.pojo.User;
import com.shika.user.utils.CodecUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "user:verify:phone:";

    public Boolean checkData(String data, Integer type) {
        User record = new User();
        switch (type) {
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                throw new SkException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }
        int i = userMapper.selectCount(record);
        return i == 0;
    }

    public void sendVerifyCode(String phone) {
        //redis:生成key
        String key = KEY_PREFIX + phone;
        //生成验证码,6位随机数字
        String code = NumberUtils.generateCode(6);
        System.out.println("请记住您的验证码：" + code);
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        //发送验证码
        amqpTemplate.convertAndSend("shika.tm.exchange", "tm.verify.code", msg);
        //将验证码保存在服务端
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        //校验验证码
        String cacheCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(code, cacheCode)) {
            throw new SkException(ExceptionEnum.VERIFY_CODE_NOT_MATCHING);
        }
        //密码加密
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        String encodedPass = CodecUtils.md5Hex(user.getPassword(), salt);//加盐
        user.setPassword(encodedPass);
        //写入数据库
        user.setCreated(new Date());
        boolean boo = userMapper.insertSelective(user) == 1;
        // 如果注册成功，删除redis中的code
        if (boo) {
            try {
                this.redisTemplate.delete(KEY_PREFIX + user.getPhone());
            } catch (Exception e) {
                log.error("删除缓存验证码失败，code：{}", code, e);
            }
        }
    }

    /**
     * 登录，根据用户名查询用户，并校验密码
     *
     * @param username
     * @param password
     * @return
     */
    public User queryUserByUsernameAndPassword(String username, String password) {
        //查询用户（只根据用户名）
        User record = new User();
        record.setUsername(username); //用户名已加唯一性索引， UNIQUE KEY `username` (`username`) USING BTREE
        User user = userMapper.selectOne(record);
        if (user == null) {
            throw new SkException(ExceptionEnum.USERNAME_OR_PASSWORD_ERROR);
        }
        //校验密码
        boolean equals = StringUtils.equals(user.getPassword(),
                CodecUtils.md5Hex(password, user.getSalt()));
        if (!equals) {
            throw new SkException(ExceptionEnum.USERNAME_OR_PASSWORD_ERROR);
        }
        return user;
    }
}
