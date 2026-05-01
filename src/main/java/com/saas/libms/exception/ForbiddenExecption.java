package com.saas.libms.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenExecption extends AppException{
    public ForbiddenExecption(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
