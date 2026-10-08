package com.vet.controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Collections.singletonMap("message",e.getMessage()));}
 @ExceptionHandler(RuntimeException.class) public ResponseEntity<Map<String,String>> runtime(RuntimeException e){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message",e.getMessage()==null?"Request failed":e.getMessage()));}
}
