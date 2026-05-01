package com.saas.libms.exception;


import com.saas.libms.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
//1 custom
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex){

        if(ex.getStatus().is5xxServerError()){
            log.error("Server error: {}", ex.getMessage());
        }
        else{
            log.warn("Client Error [{}]: {}", ex.getStatus().value(), ex.getMessage());
        }
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

 //2 validation errors, from dto validations
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
     // Collect all field errors into a simple list: [{ field: "email", message: "must not be blank" }]
     List<ValidationError> fieldErrors = ex.getBindingResult()
             .getFieldErrors()
             .stream()
             .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
             .toList();

     log.warn("Validation failed: {}", fieldErrors);
     return ResponseEntity
             .status(HttpStatus.BAD_REQUEST)
             .body(ApiResponse.validationError("Validation failed", fieldErrors));
 }

 //3
 @ExceptionHandler(HttpMessageNotReadableException.class)
 public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
     log.warn("Unreadable request body: {}", ex.getMessage());
     return ResponseEntity
             .status(HttpStatus.BAD_REQUEST)
             .body(ApiResponse.error("Request body is missing or malformed"));
 }

 //4
 @ExceptionHandler(MethodArgumentTypeMismatchException.class)
 public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
     String message = String.format(
             "Parameter '%s' has an invalid value: '%s'",
             ex.getName(), ex.getValue()
     );
     log.warn("Type mismatch: {}", message);
     return ResponseEntity
             .status(HttpStatus.BAD_REQUEST)
             .body(ApiResponse.error(message));
 }

 //5
 @ExceptionHandler(Exception.class)
 public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
     log.error("Unexpected error: {}", ex.getMessage(), ex);
     return ResponseEntity
             .status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
 }



}
