package com.saas.libms.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends  AppException{

    public ResourceNotFoundException(String mesage) {
        super(mesage, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resource, String identifier){
        super(resource + " with id '" + identifier + "' not found", HttpStatus.NOT_FOUND);
    }
}
