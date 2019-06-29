package com.shika.common.exceptions;

import com.shika.common.enums.ExceptionEnum;
import lombok.Getter;

/**
 * Created by Jiang on 2019/6/20.
 */
@Getter
public class SkException extends RuntimeException {
    private ExceptionEnum exceptionEnum;

    public SkException(ExceptionEnum exceptionEnum) {
        this.exceptionEnum = exceptionEnum;
    }
}
