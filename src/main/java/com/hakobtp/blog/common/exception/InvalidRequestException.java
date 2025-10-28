package com.hakobtp.blog.common.exception;

import com.hakobtp.blog.common.error.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Provided request is invalid")
public class InvalidRequestException extends BaseException {

    public InvalidRequestException(String errorMessage) {
        super(errorMessage);
    }

    public InvalidRequestException(ErrorMessage errorMessage) {
        super(errorMessage);
    }
}