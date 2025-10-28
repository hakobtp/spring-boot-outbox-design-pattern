package com.hakobtp.blog.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class OutboxConfigurationException extends BaseException {

    public OutboxConfigurationException(String message) {
        super(message);
    }

    public static OutboxConfigurationException configurationKeyNotFound(String configurationKey) {
        return new OutboxConfigurationException("Could not find configuration key: " + configurationKey);
    }
}
