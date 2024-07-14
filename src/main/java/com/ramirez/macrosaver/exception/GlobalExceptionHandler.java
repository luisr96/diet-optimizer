package com.ramirez.macrosaver.exception;

import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoFeasibleSolutionException.class)
    public ResponseEntity<String> handleNoFeasibleSolutionException(NoFeasibleSolutionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
