package com.nttdata.transaction.utils;

import org.openapitools.model.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<TransactionResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Error de validación: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new TransactionResponse()
                        .status(400)
                        .message(Constants.ERROR_VALIDATION_MESSAGE)
                        .transactions(null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TransactionResponse> handleGeneralException(Exception e) {
        log.error("Error inesperado: ", e);
        return ResponseEntity
                .status(500)
                .body(new TransactionResponse()
                        .status(500)
                        .message(Constants.ERROR_INTERNAL)
                        .transactions(null));
    }
}
