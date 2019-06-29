package com.shika.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.shika.common.enums.ExceptionEnum;
import com.shika.common.exceptions.SkException;
import com.shika.upload.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Jiang on 2019/6/26.
 */
@Service
@Slf4j
@EnableConfigurationProperties(UploadProperties.class)
public class UploadService {
    //FDFS客户端（使用方法见测试类）
    @Autowired
    private FastFileStorageClient storageClient;

    //自定义属性注入
    @Autowired
    private UploadProperties uploadProperties;

    public String uploadImage(MultipartFile file) {
        //校验文件类型
        if (!uploadProperties.getAllowTypes().contains(file.getContentType())) {
            throw new SkException(ExceptionEnum.INVALID_FILE_FORMAT);
        }
        try {
            //校验文件内容是否为图片格式
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                throw new SkException(ExceptionEnum.INVALID_FILE_FORMAT);
            }
            //可加类似过滤
            if (bufferedImage.getWidth() > 1000) {
                log.error("图片过宽！");
            }
            //文件目标路径
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);
            return uploadProperties.getBaseUrl() + storePath.getFullPath();
        } catch (IOException e) {
            log.error("上传图片失败！" + e);
            throw new SkException(ExceptionEnum.UPLOAD_IMAGE_EXCEPTION);
        }
    }
}
