package com.saas.libms.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Object errors;
    private final LocalDateTime timestamp;

    public ApiResponse(boolean success, String message,
                       T data, Object errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    //sucecss with data
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message,data,null);
    }


    //success without data
    public static <T> ApiResponse<T> success(String message) {
        return  new ApiResponse<>(true, message, null,null);
    }

    //errors
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false,message, null, null);
    }

    //Errors for validations
    public static <T> ApiResponse<T> validationError(String message, Object errors) {
        return  new ApiResponse<>(false, message, null, errors);
    }


}
