package com.mockarena.challenge.api;

import com.mockarena.challenge.application.InsufficientQuestionsException;
import com.mockarena.challenge.application.UncomposableChallengeVersionException;
import com.mockarena.challenge.question.QuestionCatalogUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.OptimisticLockException;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(String code, String message) { }
    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST) ErrorResponse validation(Exception exception) { return new ErrorResponse("VALIDATION_ERROR", "Request validation failed"); }
    @ExceptionHandler(InsufficientQuestionsException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse insufficient(InsufficientQuestionsException exception) { return new ErrorResponse("QUESTION_SELECTION_INSUFFICIENT", exception.getMessage()); }
    @ExceptionHandler(QuestionCatalogUnavailableException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ErrorResponse catalogUnavailable(QuestionCatalogUnavailableException exception) { return new ErrorResponse("QUESTION_CATALOG_UNAVAILABLE", "Question catalog is unavailable"); }
    @ExceptionHandler(java.util.NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse notFound(java.util.NoSuchElementException exception) { return new ErrorResponse("NOT_FOUND", exception.getMessage()); }
    @ExceptionHandler(UncomposableChallengeVersionException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse uncomposable(UncomposableChallengeVersionException exception) { return new ErrorResponse("CHALLENGE_VERSION_NOT_COMPOSABLE", exception.getMessage()); }
    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class}) @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse conflict(Exception exception) { return new ErrorResponse("VERSION_CONFLICT", "The resource was changed by another request"); }
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse invalidState(IllegalStateException exception) { return new ErrorResponse("INVALID_STATE", exception.getMessage()); }
}
