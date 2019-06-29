package com.shika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Created by Jiang on 2019/6/19.
 */
@SpringBootApplication
@EnableEurekaServer
public class SkRegistry {
    public static void main(String[] args) {
        SpringApplication.run(SkRegistry.class, args);
    }
}
