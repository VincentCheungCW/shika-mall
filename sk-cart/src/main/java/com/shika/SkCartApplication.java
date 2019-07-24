package com.shika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SkCartApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkCartApplication.class, args);
    }
}
