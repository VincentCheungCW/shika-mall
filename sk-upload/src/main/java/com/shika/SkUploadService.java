package com.shika;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Created by Jiang on 2019/6/25.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class SkUploadService {
    public static void main(String[] args) {
        SpringApplication.run(SkUploadService.class, args);
    }
}
