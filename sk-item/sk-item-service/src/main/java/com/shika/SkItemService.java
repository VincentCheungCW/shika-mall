package com.shika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * Created by Jiang on 2019/6/19.
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.shika.item.mapper")
public class SkItemService {
    public static void main(String[] args) {
        SpringApplication.run(SkItemService.class, args);
    }
}
