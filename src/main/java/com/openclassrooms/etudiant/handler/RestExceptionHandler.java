package com.openclassrooms.etudiant.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {IllegalArgumentException.class, IllegalStateException.class})
    protected ResponseEntity<Object> handleConflict(RuntimeException runtimeException, WebRequest request) {
        logError(runtimeException);
        return handleExceptionInternal(runtimeException, getErrorDetails(runtimeException, request), new HttpHeaders(),
                HttpStatus.BAD_REQUEST, request);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = {NoSuchElementException.class})
    protected ResponseEntity<Object> handleNotFound(RuntimeException runtimeException, WebRequest request) {
        logError(runtimeException);
        return handleExceptionInternal(runtimeException, getErrorDetails(runtimeException, request), new HttpHeaders(),
                HttpStatus.NOT_FOUND, request);
    }


    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(value = {BadCredentialsException.class})
    protected ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException badCredentialsException,
                                                                   WebRequest request) {
        logError(badCredentialsException);
        return handleExceptionInternal(badCredentialsException, getErrorDetails(badCredentialsException, request),
                new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(value = {AccessDeniedException.class})
    protected ResponseEntity<Object> handleForbiddenException(AccessDeniedException accessDeniedException,
                                                              WebRequest request) {
        logError(accessDeniedException);
        return handleExceptionInternal(accessDeniedException, getErrorDetails(accessDeniedException, request),
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<Object> handleException(RuntimeException runtimeException, WebRequest request) {
        logError(runtimeException);
        return handleExceptionInternal(runtimeException, "Internal Server error", new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        logError(exception);
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Validation error");
        return handleExceptionInternal(exception, new ErrorDetails(LocalDateTime.now(), message,
                request.getDescription(false)), headers, HttpStatus.BAD_REQUEST, request);
    }

    private void logError(Exception exception) {
        logger.error(exception.getMessage(), exception);
    }

    private ErrorDetails getErrorDetails(Exception exception, WebRequest request) {
        return new ErrorDetails(LocalDateTime.now(), exception.getMessage(), request.getDescription(false));
    }
}
