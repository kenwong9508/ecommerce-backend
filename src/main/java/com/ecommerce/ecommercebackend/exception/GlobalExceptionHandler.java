package com.ecommerce.ecommercebackend.exception;

import com.ecommerce.ecommercebackend.dto.common.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // Intercepts exceptions globally across all controllers
public class GlobalExceptionHandler {

  /** Handles business logic rule violations (e.g., Email already in use) */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
      IllegalArgumentException ex) {
    log.warn("Business rule violation: {}", ex.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
  }

  /** Handles DTO validation errors triggered by @Valid (e.g., Invalid email, short password) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    // Extract all field errors into a clean Map format
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    log.warn("Input validation failed: {}", errors);

    // Pass the errors map into the 'details' parameter
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("VALIDATION_ERROR", "Invalid input data format", errors));
  }

  /** Fallback handler for any unhandled runtime exceptions (Catch-all) */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
    log.error("An unexpected error occurred: ", ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponse.error(
                "INTERNAL_SERVER_ERROR",
                "An unexpected system error occurred. Please try again later."));
  }
}
