package com.shika.auth.service;

import com.shika.auth.client.UserClient;
import com.shika.auth.config.JwtProperties;
import com.shika.auth.entity.UserInfo;
import com.shika.auth.utils.JwtUtils;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProp;

    public String login(String username, String password) {
        //校验用户名密码
        User user = userClient.queryUserByUsernameAndPassword(username, password);
        if (user == null) {
            throw new SkException(ExceptionEnum.USERNAME_OR_PASSWORD_ERROR);
        }
        //生成token
        String token = JwtUtils.generateToken(new UserInfo(user.getId(), username),
                jwtProp.getPrivateKey(), jwtProp.getExpire());
        return token;
    }
}
