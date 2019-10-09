package com.shika.order.interceptor;

import com.shika.auth.entity.UserInfo;
import com.shika.auth.utils.JwtUtils;
import com.shika.common.utils.CookieUtils;
import com.shika.order.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class UserInterceptor implements HandlerInterceptor {
    private JwtProperties jwtProperties;
    // 一个请求的处理流程（Interceptor->Controller->Service）在一个线程内完成
    // 定义一个线程域，存放登录用户
    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    public UserInterceptor(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 查询token
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        if (StringUtils.isBlank(token)) {
            // 未登录,返回401
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false; //拦截
        }
        // 有token，查询用户信息
        try {
            // 解析成功，证明已经登录
            UserInfo user = JwtUtils.getUserInfo(jwtProperties.getPublicKey(), token);
            // 传递user至controller，放入线程域，也可以放入request中:request.setAttribute("user", user);
            tl.set(user);
            return true;
        } catch (Exception e) {
            // 抛出异常，证明未登录,返回401
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            log.error("[购物车服务] 解析用户身份失败", e);
            return false; //拦截
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //请求处理完毕删除threadlocal数据
        tl.remove();
    }

    /**
     * 对外提供了静态的方法：getLoginUser()来获取ThreadLocal中的User信息
     *
     * @return
     */
    public static UserInfo getLoginUser() {
        return tl.get();
    }
}
