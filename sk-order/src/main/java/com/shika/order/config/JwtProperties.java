package com.shika.order.config;

import com.shika.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "shika.jwt")
public class JwtProperties {
    private String pubKeyPath;// 公钥
    private String cookieName;
    private PublicKey publicKey; // 公钥

    /**
     * 根据密钥生成公钥和私钥，类实例化后就读取公钥和私钥到内存中
     *
     * @PostContruct：在构造方法执行之后执行该方法
     */
    @PostConstruct
    public void init() {
        // 获取公钥
        try {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
