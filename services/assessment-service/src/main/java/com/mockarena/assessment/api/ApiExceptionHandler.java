package com.mockarena.assessment.api;

import com.mockarena.assessment.application.*;
import com.mockarena.assessment.infrastructure.challenge.*;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(String code, String message) { }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class, IllegalArgumentException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse validation(Exception exception) { return new ErrorResponse("VALIDATION_ERROR", "Request validation failed"); }
    @ExceptionHandler(AssessmentNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND) ErrorResponse assessmentNotFound(AssessmentNotFoundException exception) { return new ErrorResponse("ASSESSMENT_NOT_FOUND", "Assessment not found"); }
    @ExceptionHandler(AssessmentVersionNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND) ErrorResponse versionNotFound(AssessmentVersionNotFoundException exception) { return new ErrorResponse("ASSESSMENT_VERSION_NOT_FOUND", "Assessment version not found"); }
    @ExceptionHandler(ChallengeVersionNotComposableException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) ErrorResponse uncomposable(ChallengeVersionNotComposableException exception) { return new ErrorResponse("CHALLENGE_VERSION_NOT_COMPOSABLE", "Challenge version is not composable"); }
    @ExceptionHandler(ChallengeServiceUnavailableException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) ErrorResponse unavailable(ChallengeServiceUnavailableException exception) { return new ErrorResponse("CHALLENGE_SERVICE_UNAVAILABLE", "Challenge service is unavailable"); }
    @ExceptionHandler(IdempotencyKeyReusedException.class) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse reused(IdempotencyKeyReusedException exception) { return new ErrorResponse("IDEMPOTENCY_KEY_REUSED", "Idempotency-Key was already used for another request"); }
    @ExceptionHandler(AttemptStartEntitlementDeniedException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY) ErrorResponse attemptUnavailable(AttemptStartEntitlementDeniedException exception) { return new ErrorResponse("ATTEMPT_START_NOT_ALLOWED", "Assessment attempt cannot be started"); }
    @ExceptionHandler(AttemptStartUnavailableException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) ErrorResponse attemptStartUnavailable(AttemptStartUnavailableException exception) { return new ErrorResponse("ATTEMPT_START_UNAVAILABLE", "Attempt start is temporarily unavailable"); }
    @ExceptionHandler(com.mockarena.assessment.infrastructure.question.QuestionServiceUnavailableException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) ErrorResponse questionUnavailable(RuntimeException exception) { return new ErrorResponse("QUESTION_SERVICE_UNAVAILABLE", "Question content is unavailable"); }
    @ExceptionHandler(SecurityException.class) @ResponseStatus(HttpStatus.FORBIDDEN) ErrorResponse forbidden(SecurityException exception) { return new ErrorResponse("FORBIDDEN", "Access is denied"); }
    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class}) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse conflict(Exception exception) { return new ErrorResponse("VERSION_CONFLICT", "The resource was changed by another request"); }
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse state(IllegalStateException exception) { return new ErrorResponse("INVALID_STATE", exception.getMessage()); }
}
