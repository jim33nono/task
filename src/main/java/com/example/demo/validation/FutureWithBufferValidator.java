package com.example.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;

public class FutureWithBufferValidator implements ConstraintValidator<FutureWithBuffer, Instant> {

    private long bufferSeconds;

    @Override
    public void initialize(FutureWithBuffer constraintAnnotation) {
        this.bufferSeconds = constraintAnnotation.seconds();
    }

    @Override
    public boolean isValid(Instant value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle this
        }
        Instant nowWithBuffer = Instant.now().plusSeconds(bufferSeconds);
        return value.isAfter(nowWithBuffer);
    }
}
