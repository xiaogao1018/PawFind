
package com.pawfind.backend.common.exception;

import com.pawfind.backend.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Result<Void>> handleUsernameExists(
            UsernameAlreadyExistsException e) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Result.error(409, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");

        return ResponseEntity
                .badRequest()
                .body(Result.error(400, message));
    }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<Result<Void>> handleInvalidCredentials(
                InvalidCredentialsException e) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(401, e.getMessage()));
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<Result<Void>> handleForbidden(
                ForbiddenException e) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Result.error(403, e.getMessage()));
        }

        @org.springframework.web.bind.annotation.ExceptionHandler(
                org.springframework.web.multipart.MaxUploadSizeExceededException.class
        )
        public org.springframework.http.ResponseEntity<
                com.pawfind.backend.common.result.Result<Void>
                > handleMaxUploadSize(
                org.springframework.web.multipart.MaxUploadSizeExceededException e) {

        return org.springframework.http.ResponseEntity
                .status(
                        org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE
                )
                .body(
                        com.pawfind.backend.common.result.Result.error(
                        413, "上传文件过大，单张图片不能超过5MB"
                        )
                );
        }


}
