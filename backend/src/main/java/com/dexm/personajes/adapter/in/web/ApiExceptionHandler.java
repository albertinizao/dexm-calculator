package com.dexm.personajes.adapter.in.web;
import org.springframework.web.bind.annotation.*; import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler { @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND) Map<String,String> notFound(Exception e){return Map.of("error",e.getMessage());} @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST) Map<String,String> bad(Exception e){return Map.of("error",e.getMessage());} }
