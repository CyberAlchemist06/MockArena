package com.mockarena.question.api;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(String code, String message) { }
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) ErrorResponse notFound(NoSuchElementException e) { return new ErrorResponse("NOT_FOUND", e.getMessage()); }
    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class}) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse conflict(Exception e) { return new ErrorResponse("VERSION_CONFLICT", "The resource was changed by another request"); }
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse invalidState(IllegalStateException e) { return new ErrorResponse("INVALID_STATE", e.getMessage()); }
}
