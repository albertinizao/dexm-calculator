package com.dexm.personajes.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import java.time.Instant; import java.util.NoSuchElementException; import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
 private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
 @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<ErrorResponse> notFound(NoSuchElementException e,HttpServletRequest r){return handle(HttpStatus.NOT_FOUND,"NOT_FOUND","Recurso no encontrado.",e,r);}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,ConstraintViolationException.class,MethodArgumentNotValidException.class,HandlerMethodValidationException.class}) public ResponseEntity<ErrorResponse> badRequest(Exception e,HttpServletRequest r){return handle(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","La solicitud no es válida.",e,r);}
 @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<ErrorResponse> malformed(HttpMessageNotReadableException e,HttpServletRequest r){return handle(HttpStatus.BAD_REQUEST,"MALFORMED_JSON","El cuerpo JSON no es válido.",e,r);}
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ErrorResponse> status(ResponseStatusException e,HttpServletRequest r){HttpStatus s=HttpStatus.resolve(e.getStatusCode().value()); if(s==null)s=HttpStatus.INTERNAL_SERVER_ERROR; return handle(s,s==HttpStatus.NOT_FOUND?"NOT_FOUND":"HTTP_ERROR",s==HttpStatus.NOT_FOUND?"Recurso no encontrado.":"La solicitud no se ha podido completar.",e,r);}
 @ExceptionHandler(Exception.class) public ResponseEntity<ErrorResponse> unexpected(Exception e,HttpServletRequest r){return handle(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","Se ha producido un error interno.",e,r);}
 private ResponseEntity<ErrorResponse> handle(HttpStatus s,String code,String msg,Throwable e,HttpServletRequest r){String id=id(r); log.error("api_error requestId={} method={} path={} status={} code={} exceptionType={} causeType={}",id,r.getMethod(),r.getRequestURI(),s.value(),code,e.getClass().getName(),root(e).getClass().getName(),SafeLogException.sanitize(e)); return ResponseEntity.status(s).body(new ErrorResponse(Instant.now(),s.value(),code,msg,id));}
 private static String id(HttpServletRequest r){Object id=r.getAttribute(RequestCorrelationFilter.ATTRIBUTE); if(id instanceof String value && value.matches("[A-Za-z0-9._:-]{1,128}")) return value; String mdc=org.slf4j.MDC.get(RequestCorrelationFilter.MDC_KEY); return mdc==null?UUID.randomUUID().toString():mdc;}
 private static Throwable root(Throwable e){var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>()); Throwable c=e; while(c.getCause()!=null&&c.getCause()!=c&&seen.add(c))c=c.getCause(); return c;}
}
