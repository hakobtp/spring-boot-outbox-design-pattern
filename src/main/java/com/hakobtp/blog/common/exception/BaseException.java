package com.hakobtp.blog.common.exception;

import com.hakobtp.blog.common.error.ErrorMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {
    ErrorMessage errorMessage;

    public BaseException(String message) {
        super(message);
        this.errorMessage = ErrorMessage.builder()
                .bannerMessage(message)
                .build();
    }

    public BaseException(ErrorMessage errorMessage) {
        super(errorMessage.bannerMessage());
        this.errorMessage = errorMessage;
    }
}