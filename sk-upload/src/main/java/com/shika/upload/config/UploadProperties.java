package com.shika.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 读取配置文件自定义属性
 * Created by Jiang on 2019/6/27.
 */

@Data
@ConfigurationProperties(prefix = "sk.upload")
public class UploadProperties {
    private String baseUrl;
    private List<String> allowTypes;
}
