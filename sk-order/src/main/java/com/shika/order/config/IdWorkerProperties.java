package com.shika.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "shika.worker")
public class IdWorkerProperties {

    private long workerId;// 当前机器id
    private long datacenterId;// 序列号

}