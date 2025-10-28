package com.hakobtp.blog.common.error;

import lombok.Builder;
import lombok.Singular;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Builder
public record ErrorMessage(
        String bannerMessage,
        @Singular List<FieldErrorInfo> fieldErrors
) {
    public Optional<FieldErrorInfo> findByFieldName(String fieldName) {
        return fieldErrors.stream()
                .filter(error -> Objects.equals(error.fieldName(), fieldName))
                .findFirst();
    }
}