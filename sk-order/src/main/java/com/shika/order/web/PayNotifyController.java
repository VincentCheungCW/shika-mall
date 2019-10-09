package com.shika.order.web;

import com.shika.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bystander
 * @date 2018/10/5
 */
@RestController
@Slf4j
public class PayNotifyController {

    @Autowired
    private OrderService orderService;

    /**
     * 微信支付的支付成功回调接口（微信发起的post请求）
     * 支付完成后，微信会把支付结果和用户信息发送给商户，商户需要接收处理，并返回应答。
     * 注意：微信返回的@RequestBody Map<String, String> msg是XML格式，要引入依赖jackson-dataformat-xml转为json
     *
     * @param msg
     * @return
     */
    @PostMapping(value = "/wxpay/notify", produces = "application/xml")  //返回XML
    public ResponseEntity<String> payNotify(@RequestBody Map<String, String> msg) {
        //处理回调结果
        orderService.handleNotify(msg);
        // 没有异常，则返回成功给微信
        String result = "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
        //或者使用map,因为定义了produces = "application/xml"，所以会自动转为XML
        //Map<String, String> result = new HashMap<>();
        //result.put("return_code", "SUCCESS");
        //result.put("return_msg", "OK");
        //return result;
        return ResponseEntity.ok(result);

    }
}
