package com.shika.tm.mq;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.shika.tm.config.TmProperties;
import com.shika.tm.utils.TmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@EnableConfigurationProperties(TmProperties.class)
public class sendTextListener {
    @Autowired
    private TmUtils tmUtils;
    @Autowired
    private TmProperties prop;

    /**
     * 发送短信验证码
     *
     * @param msg
     * @throws Exception
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "shika.tm.queue", durable = "true"),
            exchange = @Exchange(value = "shika.tm.exchange",
                    ignoreDeclarationExceptions = "true"),
            key = {"tm.verify.code"}))
    public void listenTm(Map<String, String> msg) {
        if (msg == null || msg.size() <= 0) {
            // 放弃处理
            return;
        }
        String phone = msg.get("phone");
        String code = msg.get("code");

        if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)) {
            // 放弃处理
            return;
        }
        // 发送消息
        // 这里面的异常需要捕获，若抛出会导致RabbitMQ失败重试
        SendSmsResponse resp = this.tmUtils.sendSms(phone, code,  //手机号，验证码
                prop.getSignName(),
                prop.getVerifyCodeTemplate());
    }
}
