package com.luxe.common.error;

import java.util.List;

public record ValidationError(String code, String message, List<FieldError> fieldErrors) {
    public ValidationError(String message, List<FieldError> fieldErrors) {
        this("VALIDATION_ERROR", message, fieldErrors);
    }
}
