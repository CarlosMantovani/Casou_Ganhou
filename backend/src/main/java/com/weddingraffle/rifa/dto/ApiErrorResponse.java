package com.weddingraffle.rifa.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors) {

    public static ApiErrorResponse withoutFieldErrors(int status, String code, String message, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, code, message, path, List.of());
    }
}
