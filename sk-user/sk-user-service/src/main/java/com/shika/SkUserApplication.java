package com.shika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.shika.user.mapper")
public class SkUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkUserApplication.class, args);
    }
}
