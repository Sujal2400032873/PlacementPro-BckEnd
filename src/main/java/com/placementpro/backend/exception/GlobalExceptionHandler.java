package com.placementpro.backend.exception;

import com.placementpro.backend.security.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, HttpServletRequest request) {
        Object requestIdObj = request.getAttribute(RequestIdFilter.REQUEST_ID);
        String requestId = requestIdObj != null ? requestIdObj.toString() : null;

        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
    }

   @ExceptionHandler(RateLimitExceededException.class)
   public ResponseEntity<?> handleRateLimitExceededException(
       RateLimitExceededException ex,
       HttpServletRequest request
   ) {
       Object requestIdObj = request.getAttribute(RequestIdFilter.REQUEST_ID);
       String requestId = requestIdObj != null ? requestIdObj.toString() : null;

       logger.warn("RATE_LIMIT_EXCEEDED ip={} type={} requestId={} retryAfter={}s",
               ex.getIpAddress(), ex.getLimitType(), requestId, ex.getRetryAfterSeconds());

       ErrorResponse errorResponse = ErrorResponse.builder()
               .timestamp(LocalDateTime.now())
               .status(HttpStatus.TOO_MANY_REQUESTS.value())
               .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
               .message(ex.getMessage())
               .path(request.getRequestURI())
               .requestId(requestId)
               .build();

       return ResponseEntity
               .status(HttpStatus.TOO_MANY_REQUESTS)
               .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
               .body(errorResponse);
   }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {

        String validationMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                validationMessage.isBlank() ? "Validation failed" : validationMessage,
                request
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);

    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(
        RuntimeException ex,
        HttpServletRequest request
    ) {

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);

    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ErrorResponse error = buildErrorResponse(
                status,
                ex.getReason(),
                request
        );

        return ResponseEntity
            .status(status)
            .body(error);

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(
        Exception ex,
        HttpServletRequest request
    ) {

        ErrorResponse error = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);

    }
}
