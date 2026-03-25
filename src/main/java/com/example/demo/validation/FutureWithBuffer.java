package com.example.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureWithBufferValidator.class)
@Documented
public @interface FutureWithBuffer {
    String message() default "Execution time must be at least {seconds} seconds in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    long seconds() default 10;
}
