package com.saas.libms.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends AppException{
    public InternalServerException(String message){
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
