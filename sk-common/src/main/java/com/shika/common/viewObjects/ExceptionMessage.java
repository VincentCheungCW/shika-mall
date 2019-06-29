package com.shika.common.viewObjects;

import com.shika.common.enums.ExceptionEnum;
import lombok.Getter;

/**
 * Created by Jiang on 2019/6/20.
 */
@Getter
public class ExceptionMessage {
    private int statusCode;
    private String message;
    private long timestamp;

    public ExceptionMessage(ExceptionEnum e) {
        this.statusCode = e.getValue();
        this.message = e.getMessage();
        this.timestamp = System.currentTimeMillis();
    }
}
