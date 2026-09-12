package com.mockarena.challenge.api;

import com.mockarena.challenge.application.InsufficientQuestionsException;
import com.mockarena.challenge.question.QuestionCatalogUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(String code, String message) { }
    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST) ErrorResponse validation(Exception exception) { return new ErrorResponse("VALIDATION_ERROR", "Request validation failed"); }
    @ExceptionHandler(InsufficientQuestionsException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse insufficient(InsufficientQuestionsException exception) { return new ErrorResponse("QUESTION_SELECTION_INSUFFICIENT", exception.getMessage()); }
    @ExceptionHandler(QuestionCatalogUnavailableException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ErrorResponse catalogUnavailable(QuestionCatalogUnavailableException exception) { return new ErrorResponse("QUESTION_CATALOG_UNAVAILABLE", "Question catalog is unavailable"); }
}
