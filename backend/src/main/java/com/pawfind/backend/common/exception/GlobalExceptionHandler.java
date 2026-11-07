
package com.pawfind.backend.common.exception;

import com.pawfind.backend.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(
            IllegalArgumentException e) {

        Result<Void> result =
                Result.error(400, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(result);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(
            ResourceNotFoundException e) {

        Result<Void> result =
                Result.error(404, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(result);
    }

}
