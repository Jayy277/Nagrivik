package org.nagrivic.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorResponse.ValidationError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new ErrorResponse.ValidationError(
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    fieldError.getDefaultMessage()
            ));
        }

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed for one or more fields",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(org.nagrivic.modules.media.storage.exception.StorageFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStorageFileNotFoundException(
            org.nagrivic.modules.media.storage.exception.StorageFileNotFoundException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(org.nagrivic.modules.media.storage.exception.StorageUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleStorageUnavailableException(
            org.nagrivic.modules.media.storage.exception.StorageUnavailableException ex,
            HttpServletRequest request
    ) {
        log.error("Storage backend unavailable at URI: {}", request.getRequestURI(), ex);
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "STORAGE_UNAVAILABLE",
                "Storage service is temporarily unavailable. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(org.nagrivic.modules.media.storage.exception.StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(
            org.nagrivic.modules.media.storage.exception.StorageException ex,
            HttpServletRequest request
    ) {
        log.error("Storage error at URI: {}", request.getRequestURI(), ex);
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "STORAGE_ERROR",
                "A storage error occurred while processing the request.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(org.nagrivic.modules.auth.exception.AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            org.nagrivic.modules.auth.exception.AuthException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                ex.getMessage() != null ? ex.getMessage() : "Access is denied",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({
            jakarta.persistence.OptimisticLockException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                "This record was modified by another administrator. Please reload and try again.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Invalid parameter: '" + ex.getName() + "'";
        if (ex.getRequiredType() != null) {
            message += " (expected format: " + ex.getRequiredType().getSimpleName() + ")";
        }

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Malformed or unreadable JSON request body",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "FILE_TOO_LARGE",
                "The uploaded file exceeds the maximum allowed upload size",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.multipart.MultipartException.class
    })
    public ResponseEntity<ErrorResponse> handleMultipartException(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Invalid or missing multipart file upload request",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(org.nagrivic.modules.moderation.ratelimit.RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            org.nagrivic.modules.moderation.ratelimit.RateLimitExceededException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMIT_EXCEEDED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at URI: {}", request.getRequestURI(), ex);
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
