package com.shika.auth.web;

import com.shika.auth.config.JwtProperties;
import com.shika.auth.entity.UserInfo;
import com.shika.auth.service.AuthService;
import com.shika.auth.utils.JwtUtils;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.utils.CookieUtils2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@EnableConfigurationProperties(JwtProperties.class)
@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    //直接使用配置文件的属性（与@ConfigurationProperties功能相同）
    @Value("${shika.jwt.cookieName}")
    private String cookieName;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录授权（用户授权）
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("login")
    public ResponseEntity<Void> login(@RequestParam("username") String username,
                                      @RequestParam("password") String password,
                                      HttpServletResponse response,
                                      HttpServletRequest request) {
        //登录
        String token = authService.login(username, password);
        if (StringUtils.isBlank(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        //将token写入cookie，客户端每次请求会自动携带
        CookieUtils2.newBuilder(response).httpOnly().request(request)
                .build(cookieName, token);
        return ResponseEntity.ok().build();
    }

    /**
     * 校验用户登录状态（用户鉴权）
     *
     * @param token
     * @return
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(@CookieValue("SK_TOKEN") String token,
                                           HttpServletResponse response,
                                           HttpServletRequest request) {
        if (StringUtils.isBlank(token)) {
            throw new SkException(ExceptionEnum.INVALID_PARAM);
        }
        //JWT解析token
        UserInfo userInfo = JwtUtils.getUserInfo(jwtProperties.getPublicKey(), token);

        //此请求代表用户活跃，要刷新token有效时间
        String tokenUpdated = JwtUtils.generateToken(userInfo, jwtProperties.getPrivateKey()
                , jwtProperties.getExpire());
        CookieUtils2.newBuilder(response).httpOnly().request(request)
                .build(cookieName, tokenUpdated);
        return ResponseEntity.ok(userInfo);
    }

    //TODO 服务授权/鉴权(解决微服务地址的对外暴露隐患，JWT+RSA)
    // 服务间的调用，与用户授权/鉴权类似
    // Feigh的拦截器，第一次请求向auth请求授权，获取token
}

//TODO 面试点
//- 如果cookie被禁用怎么办？
//    首先可以提示用户，网站必须使用cookie，不能禁用；
//    把token放入header中返回，前端js获取头信息，存入web存储（浏览器的local storage、session storage），每次请求都需要手动携带token，写入header中。（曲线救国）

//- 如果cookie被盗用怎么办？
//    cookie中存放的token不怕篡改；
//    加强身份识别（生成token时加入IP地址、mac地址等）；
//    使用https协议，防止数据泄露。

//- 如果你的微服务地址暴露怎么办？
//    首先微服务地址不易暴露，因为所有微服务都通过Zuul访问（局域网内），对外暴露的只有Zuul；
//    万一暴露呢？（内部泄密）
//    服务间鉴权。
