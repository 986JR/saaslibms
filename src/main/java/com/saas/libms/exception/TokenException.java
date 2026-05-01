package com.saas.libms.exception;

import org.springframework.http.HttpStatus;

public class TokenException extends AppException{

    public TokenException(String message)
    {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
