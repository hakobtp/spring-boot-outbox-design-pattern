package com.hakobtp.blog.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Resource Not Found.")
public class EntityWithGivenIdNotFoundException extends BaseException {

    private EntityWithGivenIdNotFoundException(String message) {
        super(message);
    }

    public static EntityWithGivenIdNotFoundException of(Long id) {
        return new EntityWithGivenIdNotFoundException("Entity with ID " + id + " was not found.");
    }
}