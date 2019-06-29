package com.shika.common.advices;

import com.shika.common.exceptions.SkException;
import com.shika.common.viewObjects.ExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 通用异常处理：利用AOP实现
 * Controller中校验request发现格式违规时，抛出SkException自定义异常，
 * SpringMVC自动捕获，进入此ExceptionHandler
 * 返回给客户一个统一格式的ResponseEntity(含状态码及描述语句)
 * Created by Jiang on 2019/6/20.
 */
@Slf4j
@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(SkException.class)
    public ResponseEntity<ExceptionMessage> handleException(SkException e) {
        return ResponseEntity.status(e.getExceptionEnum().getValue())
                .body(new ExceptionMessage(e.getExceptionEnum()));
    }
}
