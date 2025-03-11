package com.itmo.blps.labs.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.itmo.blps.labs.dto.error.ErrorResponse;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.basc.framework.lang.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error("Forbidden")
                                .message("Access Denied")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message("Authentication failed")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error("Unauthorized")
                                .message("Invalid username or password")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error("Not Found")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error("Internal Server Error")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        protected ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                return new ResponseEntity<>(
                                ErrorResponse.builder()
                                                .message(ex.getBindingResult().getAllErrors().stream()
                                                                .map(error -> {
                                                                        String fieldName = ((FieldError) error)
                                                                                        .getField();
                                                                        String errorMessage = error.getDefaultMessage();
                                                                        return "\'" + fieldName + "\':" + " "
                                                                                        + errorMessage;
                                                                })
                                                                .collect(Collectors.joining(", ")))
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.BAD_REQUEST.value())
                                                .error("Bad Request")
                                                .path(request.getRequestURI())
                                                .build(),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
                        MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {
                String paramName = ex.getName();
                @SuppressWarnings("null")
                String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
                String message = String.format("Parameter '%s' must be of type %s", paramName, requiredType);

                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(message)
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getConstraintViolations().stream()
                                                .map(violation -> String.format("'%s' %s",
                                                                violation.getPropertyPath(),
                                                                violation.getMessage()))
                                                .collect(Collectors.joining(", ")))
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex,
                        HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Bad Request")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
}
