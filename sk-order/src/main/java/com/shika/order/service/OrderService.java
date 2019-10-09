package com.shika.order.service;

import com.shika.auth.entity.UserInfo;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.common.utils.IdWorker;
import com.shika.item.pojo.Sku;
import com.shika.order.client.AddressClient;
import com.shika.order.client.GoodsClient;
import com.shika.order.dataTransferObjects.AddressDTO;
import com.shika.order.dataTransferObjects.OrderDto;
import com.shika.order.dataTransferObjects.OrderStatusEnum;
import com.shika.order.interceptor.UserInterceptor;
import com.shika.order.mapper.OrderDetailMapper;
import com.shika.order.mapper.OrderMapper;
import com.shika.order.mapper.OrderStatusMapper;
import com.shika.order.pojo.Order;
import com.shika.order.pojo.OrderDetail;
import com.shika.order.pojo.OrderStatus;
import com.shika.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.javassist.runtime.Desc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderStatusMapper orderStatusMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private PayHelper payHelper;


    // 分布式事务：调用了多个服务的方法，同步调用
    // 远程方法抛异常会导致调用方抛异常，导致回滚
    @Transactional
    public Long createOrder(OrderDto orderDto) {
        //生成全局唯一订单ID，采用雪花算法
        long orderId = idWorker.nextId();

        //填充order，订单中的用户信息数据从Token中获取，填充到order中
        Order order = new Order();
        order.setCreateTime(new Date());
        order.setOrderId(orderId);
        order.setPaymentType(orderDto.getPaymentType());
        order.setPostFee(0L);  //// TODO 调用物流信息，根据地址计算邮费

        //获取用户信息
        UserInfo user = UserInterceptor.getLoginUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getName());
        order.setBuyerRate(false);  //卖家为留言

        //收货人地址信息，应该从数据库中物流信息中获取，这里使用的是假的数据
        AddressDTO addressDTO = AddressClient.findById(orderDto.getAddressId());
        if (addressDTO == null) {
            // 商品不存在，抛出异常
            throw new SkException(ExceptionEnum.RECEIVER_ADDRESS_NOT_FOUND);
        }
        order.setReceiver(addressDTO.getName());
        order.setReceiverAddress(addressDTO.getAddress());
        order.setReceiverCity(addressDTO.getCity());
        order.setReceiverDistrict(addressDTO.getDistrict());
        order.setReceiverMobile(addressDTO.getPhone());
        order.setReceiverZip(addressDTO.getZipCode());
        order.setReceiverState(addressDTO.getState());

        //付款金额相关，首先把orderDto转化成map，其中key为skuId,值为购物车中该sku的购买数量
        Map<Long, Integer> skuNumMap = orderDto.getCarts().stream()
                .collect(Collectors.toMap(c -> c.getSkuId(), c -> c.getNum()));
        //查询商品信息，根据skuIds批量查询sku详情
        List<Sku> skus = goodsClient.querySkuByIds(new ArrayList<>(skuNumMap.keySet()));

        if (CollectionUtils.isEmpty(skus)) {
            throw new SkException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        Double totalPay = 0.0;
        //填充orderDetail
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        //遍历skus，填充orderDetail
        for (Sku sku : skus) {
            Integer num = skuNumMap.get(sku.getId());
            totalPay += num * sku.getPrice();
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setOwnSpec(sku.getOwnSpec());
            orderDetail.setSkuId(sku.getId());
            orderDetail.setTitle(sku.getTitle());
            orderDetail.setNum(num);
            orderDetail.setPrice(sku.getPrice().longValue());
            orderDetail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            orderDetails.add(orderDetail);
        }
        order.setActualPay((totalPay.longValue() + order.getPostFee()));  //todo 还要减去优惠金额
        order.setTotalPay(totalPay.longValue());

        //保存order
        orderMapper.insertSelective(order);
        //保存detail
        orderDetailMapper.insertList(orderDetails);

        //填充orderStatus
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.INIT.value());
        orderStatus.setCreateTime(new Date());
        //保存orderStatus
        orderStatusMapper.insertSelective(orderStatus);

        // 减库存（该远程调用必须放最后，若放前面，则其后语句抛异常，该方法不能回滚）
        // 这里采用取巧的方法解决分布式事务问题
        // 更通用的解决分布式事务的方式：
        // 1.分阶段提交(2PC)，汇总每个调用结果，都成功则提交，有一个失败则全部回滚
        // 2.TCC(try-confirm-cancel)
        // 3.异步确保(MQ)
        goodsClient.decreaseStock(orderDto.getCarts());


        //todo 删除购物车中已经下单的商品数据, 采用异步mq的方式通知购物车系统删除已购买的商品，传送商品ID和用户ID
        //HashMap<String, Object> map = new HashMap<>();
        //try {
        //    map.put("skuIds", skuNumMap.keySet());
        //    map.put("userId", user.getId());
        //    amqpTemplate.convertAndSend("ly.cart.exchange", "cart.delete", JsonUtils.toString(map));
        //} catch (Exception e) {
        //    log.error("删除购物车消息发送异常，商品ID：{}", skuNumMap.keySet(), e);
        //}

        log.info("生成订单，订单编号：{}，用户id：{}", orderId, user.getId());
        return orderId;
    }


    public Order queryOrderById(Long id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if(order == null){
            throw new SkException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(id);
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        order.setOrderDetails(orderDetails);
        OrderStatus orderStatus = orderStatusMapper.selectByPrimaryKey(id);
        order.setOrderStatus(orderStatus);
        return order;
    }

    public String generateUrl(Long orderId) {
        //查询订单金额
        Order order = queryOrderById(orderId);
        Integer status = order.getOrderStatus().getStatus();
        if(status != OrderStatusEnum.INIT.value()){
            throw new SkException(ExceptionEnum.ORDER_STATUS_EXCEPTION);
        }
        Long actualPay = order.getActualPay();
        OrderDetail orderDetail = order.getOrderDetails().get(0);
        String description = orderDetail.getTitle();
        return payHelper.createPayUrl(orderId, description, actualPay);
    }

    @Transactional
    public void handleNotify(Map<String, String> msg) {
        payHelper.handleNotify(msg);
    }
}
