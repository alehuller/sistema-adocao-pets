package br.com.alevh.sistema_adocao_pets.exceptions;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class CustomExceptionHandler {

        @ExceptionHandler(Exception.class)
        public final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public final ResponseEntity<ExceptionResponse> handleNotFound(NoResourceFoundException ex, WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public final ResponseEntity<ExceptionResponse> handleNotFound(ResourceNotFoundException ex,
                        WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(RequiredObjectIsNullException.class)
        public final ResponseEntity<ExceptionResponse> handleRequiredObjectIsNull(RequiredObjectIsNullException ex,
                        WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalStateException.class)
        public final ResponseEntity<ExceptionResponse> handleIllegalStateException(IllegalStateException ex,
                        WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public final ResponseEntity<ExceptionResponse> handleBadCredentialsException(BadCredentialsException ex,
                        WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(InvalidJwtAuthenticationException.class)
        public final ResponseEntity<ExceptionResponse> handleInvalidJwtAuthenticationException(
                        InvalidJwtAuthenticationException ex, WebRequest request) {
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                List.of(ex.getMessage()),
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public final ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException ex, WebRequest request) {
                // Extrai mensagens de erro dos campos
                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .toList();
                ExceptionResponse exceptionResponse = new ExceptionResponse(
                                new Date(),
                                errors,
                                request.getDescription(false));
                return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
        }
}
