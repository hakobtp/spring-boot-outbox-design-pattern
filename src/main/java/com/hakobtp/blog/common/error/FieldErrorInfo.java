package com.hakobtp.blog.common.error;

import org.springframework.validation.FieldError;

public record FieldErrorInfo(String fieldName, String message) {
    public static FieldErrorInfo from(FieldError fieldError) {
        return new FieldErrorInfo(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}