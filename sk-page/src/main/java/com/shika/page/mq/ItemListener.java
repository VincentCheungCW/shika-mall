package com.shika.page.mq;

import com.shika.page.service.PageService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemListener {

    @Autowired
    private PageService pageService;

    /**
     * - @RabbitListener：方法上的注解，声明这个方法是一个MQ消费者方法，需要指定下面的属性：
     * - bindings：指定绑定关系，可以有多个。值是@QueueBinding的数组。@QueueBinding包含下面属性：
     *  - value：这个消费者关联的队列。值是@Queue，代表一个队列
     *  - exchange：队列所绑定的交换机，值是@Exchange类型
     *  - key：队列和交换机绑定的RoutingKey
     * 类似listen这样的方法在一个类中可以写多个，就代表多个消费者。
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "page.create.queue", durable = "true"),
            exchange = @Exchange(name = "shika.item.exchange", type = ExchangeTypes.TOPIC),
            key = {"item.insert", "item.update"}))
    public void listenInsertOrUpdate(Long spuId) {
        if(spuId == null){
            return;
        }
        //创建静态页
        pageService.createHtml(spuId);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "page.delete.queue", durable = "true"),
            exchange = @Exchange(name = "shika.item.exchange", type = ExchangeTypes.TOPIC),
            key = {"item.delete"}))
    public void listenDelete(Long spuId) {
        if(spuId == null){
            return;
        }
        //删除静态页
        pageService.deleteHtml(spuId);
    }
}
