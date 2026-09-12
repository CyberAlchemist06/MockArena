package com.mockarena.identity.api;
import com.mockarena.identity.application.*; import org.springframework.http.*; import org.springframework.web.bind.*; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {record ErrorResponse(String code,String message){}
 @ExceptionHandler({MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST) ErrorResponse validation(Exception e){return new ErrorResponse("VALIDATION_ERROR","Request validation failed");}
 @ExceptionHandler(DuplicateEmailException.class) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse duplicate(DuplicateEmailException e){return new ErrorResponse("EMAIL_ALREADY_REGISTERED","Email is already registered");}
 @ExceptionHandler(InvalidCredentialsException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED) ErrorResponse credentials(InvalidCredentialsException e){return new ErrorResponse("INVALID_CREDENTIALS","Invalid credentials");}
 @ExceptionHandler(java.util.NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) ErrorResponse missing(java.util.NoSuchElementException e){return new ErrorResponse("NOT_FOUND","Resource not found");}}
