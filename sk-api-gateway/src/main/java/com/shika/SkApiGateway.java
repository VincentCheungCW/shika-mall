package com.shika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * Created by Jiang on 2019/6/19.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
public class SkApiGateway {
    public static void main(String[] args) {
        SpringApplication.run(SkApiGateway.class, args);
    }
}
