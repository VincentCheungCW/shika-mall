package com.shika.item.service;


import com.shika.common.dto.CartDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GoodsServiceTest {

    @Autowired
    private GoodsService goodsService;

    @Test
    public void decreaseStock() throws Exception {
        List<CartDto> cartDtos = Arrays.asList(
                new CartDto(2600242L, 2),
                new CartDto(2600248L, 2));
        for(int i = 1; i < 10; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    goodsService.decreaseStock(cartDtos);
                }
            }).start();
        }
        Thread.sleep(100000);
    }
}